package db4e.controller;

import db4e.data.Category;
import db4e.data.Entry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.ResourceUtils;
import sheepy.util.Utils;

class Exporter {

//   private static final Logger log = Main.log;

   public static AtomicBoolean stop = new AtomicBoolean();

   void writeCatalog ( String root, List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 320 );
      try ( OutputStreamWriter writer = openStream( root + "/catalog.js" ) ) {
         buffer.append( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories ) {
            str( buffer, category.id.toLowerCase() ).append( ':' ).append( category.getExportCount() ).append( ',' );
         }
         write( "})", writer, buffer );
      }
   }

   void writeCategory ( String root, Category category, ProgressState state ) throws IOException, InterruptedException {
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
            for ( Object field : entry.meta )
               str( buffer, field.toString() ).append( ',' );
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

   void writeIndex ( String target, List<Category> categories ) throws IOException {
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
            buffer.setLength( buffer.length() - 1 );
            buffer.append( "]," );
         }
      }

      try ( OutputStreamWriter writer = openStream( target + "/index.js" ) ) {
         write( "})", writer, buffer );
      }
   }

   void testViewerExists () throws IOException {
      ResourceUtils.getText( "res/4e_database.html" );
   }

   void writeViewer ( String root, File target ) throws IOException {
      new File( root + "res" ).mkdir();
      copyRes( root + "res/icon.png", "res/icon.png" );
      copyRes( target.getPath(), "res/4e_database.html" );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Raw data export
   /////////////////////////////////////////////////////////////////////////////

   void writeRawCatalog ( String root, List<Category> categories, File target ) throws IOException {
      final String template = ResourceUtils.getText( "res/export_list.html" );

      final StringBuilder index_body = new StringBuilder();
      final String folder = new File( root ).getName() + "/";

      for ( Category category : categories ) {
         if ( category.entries.isEmpty() ) continue;

         index_body.append( "<tr><td><a href='" ).append( folder ).append( category.id ).append( ".html'>" ).append( Utils.escapeHTML( category.getName() ) ).append( "</a></td>" );
         index_body.append( "<td>" ).append( category.entries.stream().filter( e -> e.contentDownloaded ).count() ).append( "</td></tr>" );

         final StringBuilder head = new StringBuilder( "<th>Name</th>");
         for ( String field : category.fields )
            head.append( "<th>" ).append( Utils.escapeHTML( field ) ).append( "</th>" );

         final StringBuilder body = new StringBuilder();
         final String cat_id = category.id.toLowerCase() + "/";
         for ( Entry entry : category.entries ) {
            body.append( "<tr><td><a href='" ).append( cat_id ).append( entry.id.replace( ".aspx?id=", "-" ) ).append( ".html'>" );
            body.append( Utils.escapeHTML( entry.name ) ).append( "</a></td>" );
            for ( String field : entry.fields )
               body.append( "<td>" ).append( Utils.escapeHTML( field ) ).append( "</td>" );
            body.append( "</tr>" );
         }

         String output = template;
         output = output.replace( "[title]", Utils.escapeHTML( category.getName() ) );
         output = output.replace( "[head]", head );
         output = output.replace( "[body]", body );
         try ( OutputStreamWriter writer = openStream( root + category.id + ".html" ) ) {
            write( writer, output );
         }
      }

      // Output index
      String output = template;
      output = output.replace( "[title]", "4e Compendium Data" );
      output = output.replace( "[head]", "<th>Category</th><th>Count</th>" );
      output = output.replace( "[body]", index_body );
      try ( OutputStreamWriter writer = openStream( target.toString() ) ) {
         write( writer, output );
      }
   }

   void writeRawCategory ( String root, Category category, ProgressState state ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      String template = ResourceUtils.getText( "res/export_entry.html" );
      String cat_id = category.id.toLowerCase();
      new File( root + cat_id ).mkdirs();

      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;

         if ( stop.get() ) throw new InterruptedException();
         String output = template;
         output = output.replace( "[title]", Utils.escapeHTML( entry.name ) );
         output = output.replace( "[body]", entry.content );
         try ( OutputStreamWriter writer = openStream( root + cat_id + "/" + entry.id.replace( ".aspx?id=", "-" ) + ".html" ) ) {
            write( writer, output );
         }
         state.addOne();
      }
   }

   void testRawViewerExists () throws IOException {
      ResourceUtils.getText( "res/export_list.html" );
      ResourceUtils.getText( "res/export_entry.html" );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private OutputStreamWriter openStream ( String path ) throws FileNotFoundException {
      return new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( path, false ) ), StandardCharsets.UTF_8 );
   }

   private void write ( Writer writer, String buf ) throws IOException {
      writer.write( buf );
   }

   private void write ( CharSequence postfix, Writer writer, StringBuilder buf ) throws IOException {
      if ( ! ( postfix.charAt( 0 ) == ',' ) )
         buf.setLength( buf.length() - 1 ); // Remove last comma if postfix does not start with comma
      buf.append( postfix );
      writer.write( buf.toString() );
      buf.setLength( 0 );
   }

   private StringBuilder str ( StringBuilder buf, String txt ) {
      return buf.append( '"' ).append( js( txt ) ).append( '"' );
   }

   private String js ( String in ) {
      return in.replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
   }

   void copyRes ( String target, String source ) throws IOException {
      try ( OutputStream out = new FileOutputStream( target, false );
            InputStream in = ResourceUtils.getStream( source );
              ) {
         byte[] buffer =  new byte[ 32768 ];
         for ( int length ; (length = in.read( buffer ) ) != -1; )
            out.write( buffer, 0, length );
      }
   }
}