/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db4e.exporter;

import SevenZip.Compression.LZMA.Encoder;
import db4e.controller.ProgressState;
import db4e.converter.Convert;
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
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Base85;
import sheepy.util.ResourceUtils;

/**
 * Export viewer and data.
 */
public class ExporterMain extends Exporter {

   public static final AtomicBoolean compress = new AtomicBoolean( false ); // False for plain text json data files, true for LZMA + Base85.
   private static final int FILE_PER_CATEGORY = 20;

   private String root;

   @Override public synchronized void setState ( File target, Consumer<String> stopChecker, ProgressState state ) {
      super.setState( target, stopChecker, state );
      root = target.toString().replaceAll( "\\.html$", "" ) + "_files/";
   }

   @Override protected void _preExport ( List<Category> categories ) throws IOException {
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

   @Override protected void _postExport ( List<Category> categories ) throws IOException, InterruptedException {
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

   @Override protected void _export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
      String cat_id = category.id.toLowerCase();

      File catPath = new File( root + "/" + cat_id + "/" );
      catPath.mkdir();
      int exported = 0;
      Matcher regxIdGroup = Pattern.compile( "\\d+$" ).matcher( "" );

      StringBuilder buffer = new StringBuilder( 8192 );

      // Listing
      str( buffer, cat_id ).append( ",[\"ID\",\"Name\"," );
      for ( String header : category.fields )
         str( buffer, header ).append( ',' );
      final String listCol = backspace( buffer ).append( "]," ).toString();
      buffer.setLength( 0 );
      buffer.append( '[' );
      for ( Entry entry : category.entries ) {
         str( buffer.append( '[' ), entry.getId() ).append( ',' );
         str( buffer, entry.getName() ).append( ',' );
         for ( Object field : entry.getFields() ) {
            if ( field.getClass().isArray() ) {
               Object[] ary = (Object[]) field;
               buffer.append( "[\"" ).append( ary[0] ).append( "\"," );
               for ( int i = 1, len = ary.length ; i < len ; i++ )
                  buffer.append( ary[i] ).append( ',' );
               backspace( buffer ).append( "]," );
            } else
               str( buffer, field.toString() ).append( ',' );
         }
         backspace( buffer ).append( "]," );
      }
      try ( OutputStreamWriter writer = openStream( catPath + "/_listing.js" ) ) {
         writeData( writer, "od.reader.jsonp_data_listing(20130703," + listCol, backspace( buffer ).append( ']' ), ")" );
      }

      // Text Index
      Convert converter = Convert.getConverter( category );
      str( buffer, cat_id ).append( ',' );
      final String textCat = buffer.toString();
      buffer.setLength( 0 );
      buffer.append( '{' );
      for ( Entry entry : category.entries ) {
         String fulltext = converter.textData( entry.getContent() );
         buffer.ensureCapacity( buffer.length() + entry.getId().length() + fulltext.length() + 12 );
         str( buffer, entry.getId() ).append( ':' );
         str( buffer, fulltext ).append( ',' );
      }
      try ( OutputStreamWriter writer = openStream( catPath + "/_index.js" ) ) {
         writeData( writer, "od.reader.jsonp_data_index(20130616," + textCat, backspace( buffer ).append( '}' ), ")" );
      }
      buffer = null;
      state.add( category.entries.size() );

      StringBuilder[] data = new StringBuilder[ FILE_PER_CATEGORY ];
      int[] dataCount = new int[ FILE_PER_CATEGORY ];
      for ( Entry entry : category.entries ) {
         if ( ! regxIdGroup.reset( entry.getId() ).find() )
            throw new IllegalStateException( "Invalid id " + entry.getId() );
         int grp = Integer.parseUnsignedInt( regxIdGroup.group() ) % FILE_PER_CATEGORY;

         if ( data[ grp ] == null )
            data[ grp ] = new StringBuilder( 4096 ).append( "{" );
         data[grp].ensureCapacity( data[grp].length() + entry.getId().length() + entry.getContent().length() + 12 );
         str( data[ grp ], entry.getId() ).append( ':' );
         str( data[ grp ], entry.getContent() ).append( ',' );
         ++exported;

         if ( stop.get() ) throw new InterruptedException();
         ++dataCount[ grp ];
      }

      for ( int i = 0 ; i < data.length ; i++ ) {
         if ( data[ i ] == null ) continue;
         try ( OutputStreamWriter writer = openStream( catPath + "/data" + i + ".js" ) ) {
            writeData( writer, "od.reader.jsonp_batch_data(20160803," + textCat, backspace( data[ i ] ).append( '}' ), ")" );
         }
         data[ i ] = null;
         state.add( dataCount[ i ] );
      }

      if ( exported != category.getExportCount() )
         throw new IllegalStateException( category.id + " entry exported " + category.entries.size() + " mismatch with total " + category.getExportCount() );
   }

   private void writeIndex ( String target, List<Category> categories ) throws IOException, InterruptedException {
      Map<String, List<String>> index = new HashMap<>();
      for ( Category category : categories ) synchronized ( category ) {
         if ( index.isEmpty() )
            index.putAll( category.index );
         else if ( category.index != null )
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

   private Map<Thread, Encoder> encoders = new WeakHashMap<>( 8, 1.0f );

   private byte[] lzma ( CharSequence txt ) throws IOException {
      byte[] data = txt.toString().getBytes( UTF_8 );
      ByteArrayOutputStream buffer = new ByteArrayOutputStream( data.length / 2 ); // Only a few poisons data has a lower compression rate
      Encoder encoder = encoders.get( Thread.currentThread() );
      if ( encoder == null ) {
         encoder = new Encoder();
         encoder.SetEndMarkerMode( true );
         encoder.SetNumFastBytes( 256 );
         //encoder.SetDictionarySize( 28 ); // Default 23 = 8M. Max = 28 = 256M.
         encoders.put( Thread.currentThread(), encoder );
      }
      try ( ByteArrayInputStream inStream = new ByteArrayInputStream( data ) ) {
         int fileSize = data.length;
         encoder.WriteCoderProperties( buffer );
         for ( int i = 0; i < 8; i++ )
            buffer.write( ( fileSize >>> (8 * i) ) & 0xFF );
         encoder.Code( inStream, buffer, -1, -1, null );
      }
      return buffer.toByteArray();
   }

   private void writeData ( Writer writer, String prefix, StringBuilder data, String postfix ) throws IOException, InterruptedException {
      final int total_size = prefix.length() + data.length() + postfix.length();
      if ( data.length() <= 0 ) log.log( Level.WARNING, "Zero bytes data {0}", prefix );
      if ( stop.get() ) throw new InterruptedException();
      final String snippet = data.substring( 0, Math.min( data.length(), 20 ) );
      if ( compress.get() ) {
         byte[] zipped = lzma( data );
         String compressed = Base85.getRfc1942Encoder().encodeToString( zipped );
         if ( compressed.length() <= 0 ) log.log( Level.WARNING, "Zero bytes encoded {0}", prefix );
         final int zipped_size = prefix.length() + compressed.length() + 2 + postfix.length();
         if ( zipped_size < total_size * 0.96 ) { // Don't waste decompression time on low compression or negative compression
            writer.write( prefix + "\"" );
            writer.write( compressed );
            writer.write( '"' );
            writer.write( postfix );
            log.log( Level.FINE, "Written {0} bytes ({1,number,percent}) compressed ({2})", new Object[]{ zipped_size, (float) zipped_size / total_size, snippet } );
            data.setLength( 0 );
            return;
         }
      }
      writer.write( prefix );
      writer.write( data.toString() );
      writer.write( postfix );
      log.log( Level.FINE, "Written {0} bytes uncompressed ({1})", new Object[]{ total_size, snippet } );
      data.setLength( 0 );
   }

   private void testViewerExists () throws IOException {
      ResourceUtils.getText( "res/script.js" );
      ResourceUtils.getText( "res/style.css" );
      ResourceUtils.getText( "res/manifest.json" );
      ResourceUtils.getText( "res/4e_database.html" );
   }

   private void writeViewer ( String root, File target ) throws IOException {
      new File( root + "res" ).mkdir();
      Files.copy( ResourceUtils.getStream( "res/script.js" ), new File( root + "res/script.js" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/style.css" ), new File( root + "res/style.css" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/viewer_category_icon.png" ), new File( root + "res/viewer_category_icon.png" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/icon.png" ), new File( root + "res/icon.png" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      Files.copy( ResourceUtils.getStream( "res/manifest.json" ), new File( root + "res/manifest.json" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
      String html = ResourceUtils.getText( "res/4e_database.html" );
      if ( ! target.getName().startsWith( "4e_database." ) )
         html = html.replace( "4e_database_files", target.getName().split( "\\.", 2 )[0] + "_files" );
      Files.copy( new ByteArrayInputStream( html.getBytes( UTF_8 ) ), target.toPath(), StandardCopyOption.REPLACE_EXISTING );
   }
}