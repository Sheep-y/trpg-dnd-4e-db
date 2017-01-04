/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db4e.exporter;

import db4e.controller.Controller;
import db4e.controller.ProgressState;
import db4e.converter.Convert;
import db4e.converter.Converter;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.ResourceUtils;

/**
 * Export viewer and data.
 */
public class ExporterMain extends Exporter {

   private String root;

   @Override public void setState ( File target, Consumer<String> stopChecker, ProgressState state ) {
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
      writeCatalog( root, categories );
      state.total = categories.stream().mapToInt( e -> e.getExportCount() ).sum() * 2;
   }

   @Override public Controller.RunExcept export ( Category category ) throws IOException {
      Converter converter = Convert.getConverter( category );
      if ( converter == null ) return null;
      return () -> { synchronized( category ) {
         converter.convert( state );
         log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         writeCategory( root, category, state );
      } };
   }

   @Override public void postExport ( List<Category> categories ) throws IOException {
      checkStop( "Writing viewer" );
      writeIndex( root, categories );
      writeViewer( root, target );
   }

   private void writeCatalog ( String root, List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 320 );
      try ( OutputStreamWriter writer = openStream( root + "/catalog.js" ) ) {
         buffer.append( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories ) {
            str( buffer, category.id.toLowerCase() ).append( ':' ).append( category.getExportCount() ).append( ',' );
         }
         write( "})", writer, buffer );
      }
   }

   private void writeCategory ( String root, Category category, ProgressState state ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      String cat_id = category.id.toLowerCase();

      StringBuilder buffer = new StringBuilder( 1024 );
      File catPath = new File( root + "/" + cat_id + "/" );
      catPath.mkdir();
      int exported = 0;
      OutputStreamWriter[] writers = new OutputStreamWriter[ 100 ];
      Matcher regxIdGroup = Pattern.compile( "^([a-z]+).*?(\\d{1,2})$" ).matcher( "" );

      try ( OutputStreamWriter listing = openStream( catPath + "/_listing.js" );
            OutputStreamWriter   index = openStream( catPath + "/_index.js" );
               ) {

         // List header
         buffer.append( "od.reader.jsonp_data_listing(20130703," );
         str( buffer, cat_id ).append( ",[\"ID\",\"Name\"," );
         for ( String header : category.meta )
            str( buffer, header ).append( ',' );
         write( "],[", listing, buffer );

         // Index header
         buffer.append( "od.reader.jsonp_data_index(20130616," );
         str( buffer, cat_id );
         write( ",{", index, buffer );

         for ( Entry entry : category.sorted ) {
            // Add to listing
            str( buffer.append( '[' ), entry.shortid ).append( ',' );
            str( buffer, entry.display_name ).append( ',' );
            for ( Object field : entry.meta ) {
               if ( field.getClass().isArray() ) {
                  Object[] ary = (Object[]) field;
                  buffer.append( "[\"" ).append( ary[0] ).append( "\"," );
                  for ( int i = 1, len = ary.length ; i < len ; i++ )
                     buffer.append( ary[i] ).append( ',' );
                  backspace( buffer ).append( "]," );
               } else
                  str( buffer, field.toString() ).append( ',' );
            }
            write( "],", listing, buffer );

            // Add to full text
            str( buffer, entry.shortid ).append( ':' );
            str( buffer, entry.fulltext );
            write( ",", index, buffer );

            // Group content
            if ( ! regxIdGroup.reset( entry.shortid ).find() )
               throw new IllegalStateException( "Invalid id " + entry.shortid );
            int grp = Integer.parseUnsignedInt( regxIdGroup.group( 2 ) );

            // Write content
            if ( writers[ grp ] == null ) {
               writers[ grp ] = openStream( catPath + "/data" + grp + ".js" );
               buffer.append( "od.reader.jsonp_batch_data(20160803," );
               str( buffer, cat_id );
               write( ",{", writers[grp], buffer );
            }
            str( buffer, entry.shortid ).append( ':' );
            str( buffer, entry.data );
            write( ",", writers[grp], buffer );
            ++exported;

            if ( stop.get() ) throw new InterruptedException();
            state.addOne();
         }

         listing.write( "])" );
         index.write( "})" );

      } finally {
         // Close content
         for ( OutputStreamWriter writer : writers )
            if ( writer != null ) {
               writer.write( "})" );
               writer.close();
            }
      }
      if ( exported != category.getExportCount() )
         throw new IllegalStateException( category.id + " entry exported " + category.sorted.length + " mismatch with total " + category.getExportCount() );
   }

   private void writeIndex ( String target, List<Category> categories ) throws IOException {
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

      StringBuilder buffer = new StringBuilder( 810_000 );
      buffer.append( "od.reader.jsonp_name_index(20160808,{" );
      for ( String name : names ) {
         str( buffer, name ).append( ':' );
         List<String> ids = index.get( name );
         if ( ids.size() == 1 )
            str( buffer, ids.get(0) ).append( ',' );
         else {
            buffer.append( '[' );
            for ( String id : ids ) str( buffer, id ).append( ',' );
            backspace( buffer ).append( "]," );
         }
      }

      try ( OutputStreamWriter writer = openStream( target + "/index.js" ) ) {
         write( "})", writer, buffer );
      }
   }

   private void testViewerExists () throws IOException {
      ResourceUtils.getText( "res/4e_database.html" );
   }

   private void writeViewer ( String root, File target ) throws IOException {
      new File( root + "res" ).mkdir();
      copyRes( root + "res/icon.png", "res/icon.png" );
      copyRes( target.getPath(), "res/4e_database.html" );
   }


}
