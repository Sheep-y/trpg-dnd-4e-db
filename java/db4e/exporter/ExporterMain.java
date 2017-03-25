/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db4e.exporter;

import SevenZip.Compression.LZMA.Encoder;
import db4e.controller.ProgressState;
import db4e.converter.Convert;
import db4e.converter.Converter;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Ascii85;
import sheepy.util.ResourceUtils;

/**
 * Export viewer and data.
 */
public class ExporterMain extends Exporter {

   public static final AtomicBoolean compress = new AtomicBoolean( false ); // False for plain text json data files, true for LZMA + Base85.

   private String root;

   @Override public synchronized void setState ( File target, Consumer<String> stopChecker, ProgressState state ) {
      super.setState( target, stopChecker, state );
      root = target.toString().replaceAll( "\\.html$", "" ) + "_files/";
   }

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export target: {0}", target );
      try {
         testViewerExists();
      } catch ( IOException ex ) {
         throw new FileNotFoundException( "No viewer. Run ant make-viewer." );
      }
      new File( root ).mkdirs();
      writeCatalog( categories );
      state.total = categories.stream().mapToInt( e -> e.getExportCount() ).sum() * 2;
   }

   @Override public void export ( Category category ) throws IOException, InterruptedException {
      Converter converter = Convert.getConverter( category );
      if ( converter == null ) return;
      converter.convert( state );
      writeCategory( category );
   }

   @Override public void postExport ( List<Category> categories ) throws IOException, InterruptedException {
      checkStop( "Writing viewer" );
      writeIndex( root, categories );
      writeViewer( root, target );
   }

   private void writeCatalog ( List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 320 );
      try ( OutputStreamWriter writer = openStream( root + "/catalog.js" ) ) {
         buffer.append( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories ) {
            str( buffer, category.id.toLowerCase() ).append( ':' ).append( category.getExportCount() ).append( ',' );
         }
         write( "})", writer, buffer );
      }
   }

   private void writeCategory ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
      String cat_id = category.id.toLowerCase();

      File catPath = new File( root + "/" + cat_id + "/" );
      catPath.mkdir();
      int exported = 0;
      Matcher regxIdGroup = Pattern.compile( "^([a-z]+).*?(\\d{1,2})$" ).matcher( "" );

      StringBuilder buffer = new StringBuilder( 1024 );

      // List header
      str( buffer, cat_id ).append( ",[\"ID\",\"Name\"," );
      for ( String header : category.meta )
         str( buffer, header ).append( ',' );
      final String listCol = backspace( buffer ).append( "]," ).toString();
      buffer.setLength( 0 );

      // Index header
      str( buffer, cat_id ).append( ',' );
      final String textCat = buffer.toString();
      buffer = null;

      StringBuilder listingBuffer = new StringBuilder( 2048 ).append( '[' );
      StringBuilder textIndexBuffer = new StringBuilder( 4096 ).append( '{' );
      StringBuilder[] data = new StringBuilder[ 20 ];

      for ( Entry entry : category.sorted ) {
         // Add to listing
         str( listingBuffer.append( '[' ), entry.shortid ).append( ',' );
         str( listingBuffer, entry.display_name ).append( ',' );
         for ( Object field : entry.meta ) {
            if ( field.getClass().isArray() ) {
               Object[] ary = (Object[]) field;
               listingBuffer.append( "[\"" ).append( ary[0] ).append( "\"," );
               for ( int i = 1, len = ary.length ; i < len ; i++ )
                  listingBuffer.append( ary[i] ).append( ',' );
               backspace( listingBuffer ).append( "]," );
            } else
               str( listingBuffer, field.toString() ).append( ',' );
         }
         backspace( listingBuffer ).append( "]," );

         // Add to full text
         str( textIndexBuffer, entry.shortid ).append( ':' );
         str( textIndexBuffer, entry.fulltext );
         textIndexBuffer.append( ',' );

         // Group content
         if ( ! regxIdGroup.reset( entry.shortid ).find() )
            throw new IllegalStateException( "Invalid id " + entry.shortid );
         int grp = Integer.parseUnsignedInt( regxIdGroup.group( 2 ) ) % 20;

         // Main content
         if ( data[ grp ] == null )
            data[ grp ] = new StringBuilder( 4096 ).append( "{" );
         str( data[ grp ], entry.shortid ).append( ':' );
         str( data[ grp ], entry.data ).append( ',' );
         ++exported;

         if ( stop.get() ) throw new InterruptedException();
         state.addOne();
      }

      for ( int i = 0 ; i < data.length ; i++ ) {
         if ( data[ i ] == null ) continue;
         try ( OutputStreamWriter writer = openStream( catPath + "/data" + i + ".js" ) ) {
            writeData( writer, "od.reader.jsonp_batch_data(20160803," + textCat, backspace( data[ i ] ).append( '}' ), ")" );
         }
         data[ i ] = null;
      }

      try ( OutputStreamWriter listing = openStream( catPath + "/_listing.js" );
            OutputStreamWriter   index = openStream( catPath + "/_index.js" );
               ) {
         writeData( listing, "od.reader.jsonp_data_listing(20130703," + listCol, backspace( listingBuffer ).append( ']' ), ")" );
         writeData(   index, "od.reader.jsonp_data_index(20130616," + textCat, backspace( textIndexBuffer ).append( '}' ), ")" );
      }

      if ( exported != category.getExportCount() )
         throw new IllegalStateException( category.id + " entry exported " + category.sorted.length + " mismatch with total " + category.getExportCount() );
   }

   private void writeIndex ( String target, List<Category> categories ) throws IOException, InterruptedException {
      Map<String, List<String>> index = new HashMap<>();
      for ( Category category : categories ) synchronized ( index ) {
         if ( index.isEmpty() )
            index.putAll( category.index );
         else
            category.index.entrySet().forEach( ( entry ) -> {
               List<String> list = index.get( entry.getKey() );
               if ( list == null )
                  index.put( entry.getKey(), new ArrayList<>( entry.getValue() ) );
               else
                  list.addAll( entry.getValue() );
            });
      }

      String[] names = index.keySet().toArray( new String[ index.size() ] );
      Arrays.sort( names, ( a, b ) -> {
         int diff = b.length() - a.length();
         if ( diff != 0 ) return diff;
         return a.compareTo( b );
      });


      StringBuilder index_buffer = new StringBuilder( 810_000 );
      index_buffer.append( '{' );
      for ( String name : names ) {
         str( index_buffer, name ).append( ':' );
         List<String> ids = index.get( name );
         if ( ids.size() == 1 )
            str( index_buffer, ids.get(0) ).append( ',' );
         else {
            index_buffer.append( '[' );
            for ( String id : ids ) str( index_buffer, id ).append( ',' );
            backspace( index_buffer ).append( "]," );
         }
      }
      backspace( index_buffer ).append( '}' );

      try ( OutputStreamWriter writer = openStream( target + "/index.js" ) ) {
         writeData( writer, "od.reader.jsonp_name_index(20160808,", index_buffer, ")" );
      }
   }

   private byte[] lzma ( CharSequence txt ) throws IOException {
      byte[] data = txt.toString().getBytes( StandardCharsets.UTF_8 );
      ByteArrayOutputStream buffer = new ByteArrayOutputStream( data.length / 2 );
      try ( ByteArrayInputStream inStream = new ByteArrayInputStream( data ) ) {
         Encoder encoder = new Encoder();
         encoder.SetEndMarkerMode( true );
         encoder.SetNumFastBytes( 256 );
         //encoder.SetDictionarySize( 28 ); // Default 23 = 8M. Max = 28 = 256M.
         int fileSize = data.length;
         encoder.WriteCoderProperties( buffer );
         for (int i = 0; i < 8; i++)
            buffer.write( ( fileSize >>> (8 * i) ) & 0xFF );
         encoder.Code( inStream, buffer, -1, -1, null );
      }
      return buffer.toByteArray();
   }

   private void writeData ( Writer writer, String prefix, StringBuilder data, String postfix ) throws IOException, InterruptedException {
      final int total_size = prefix.length() + data.length() + postfix.length();
      if ( data.length() <= 0 ) log.log( Level.WARNING, "Zero bytes data {0}", prefix );
      if ( compress.get() ) {
         byte[] zipped = lzma( data );
         String compressed = Ascii85.encode( zipped );
         if ( compressed.length() <= 0 ) log.log( Level.WARNING, "Zero bytes encoded {0}", prefix );
         final int zipped_size = prefix.length() + compressed.length() + 2 + postfix.length();
         writer.write( prefix + "\"" );
         writer.write( compressed );
         writer.write( '"' );
         log.log( Level.FINE, "Written {0} bytes ({1,number,percent}) compressed ({2})", new Object[]{ zipped_size, (float) zipped_size / total_size, prefix } );
      } else {
         writer.write( prefix );
         writer.write( data.toString() );
         log.log( Level.FINE, "Written {0} bytes uncompressed ({1})", new Object[]{ total_size, prefix } );
      }
      data.setLength( 0 );
      writer.write( postfix );
      if ( stop.get() ) throw new InterruptedException();
   }

   private void testViewerExists () throws IOException {
      ResourceUtils.getText( "res/4e_database.html" );
   }

   private void writeViewer ( String root, File target ) throws IOException {
      new File( root + "res" ).mkdir();
      Files.copy( ResourceUtils.getStream( "res/icon.png" ), new File( root + "res/icon.png" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/manifest.json" ), new File( root + "res/manifest.json" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/4e_database.html" ), target.toPath(), StandardCopyOption.REPLACE_EXISTING );
   }
}
