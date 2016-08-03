package db4e.controller;

import db4e.Main;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.ResourceUtils;

class Exporter {

   private static final Logger log = Main.log;

   void writeCatalog ( String target, List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 300 );
      try ( OutputStreamWriter writer = openStream( target + "/catalog.js" ) ) {
         buffer.append( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories ) {
            if ( category.getExportCount() > 0 )
               str( buffer, category.id ).append( ':' ).append( category.getExportCount() ).append( ',' );
         }
         write( "})", writer, buffer );
      }
   }

   Matcher regxIdGroup = Pattern.compile( "/([a-z]+).*?(\\d{1,2})/" ).matcher( "" );

   void writeCategory ( String target, Category category, ProgressState state ) throws IOException {
      if ( category.total_entry.get() + category.exported_entry_deviation.get() <= 0 ) return;
      StringBuilder buffer = new StringBuilder( 1024 );
      File catPath = new File( target + "/" + category.id.toLowerCase() + "/" );
      catPath.mkdir();
      int exported = 0;

      try ( OutputStreamWriter listing = openStream( catPath + "/_listing.js" );
            OutputStreamWriter   index = openStream( catPath + "/_index.js" );
              ) {

         // List header
         buffer.append( "od.reader.jsonp_data_listing(20130703," );
         str( buffer, category.id ).append( ",[\"ID\",\"Name\"," );
         for ( String header : category.meta )
            str( buffer, header ).append( ',' );
         write( "],[", listing, buffer );

         // Index header
         buffer.append( "od.reader.jsonp_data_index(20130616," );
         str( buffer, category.id );
         write( ",{", index, buffer );

         OutputStreamWriter[] writers = new OutputStreamWriter[ 100 ];

         try {
            for ( Entry entry : category.sorted ) {
               if ( ! "null".equals( entry.shortid ) ) {
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
                  if ( writers[ grp ] == null ) {
                     writers[ grp ] = openStream( catPath + "/" + regxIdGroup.group( 1 ) + grp + ".js" );
                     buffer.append( "od.reader.jsonp_batch_data(20160803," );
                     str( buffer, category.id ).append( ',' );
                     write( "{", writers[grp], buffer );
                  }
                  str( buffer, entry.shortid ).append( ':' );
                  str( buffer, entry.data ).append( ',' );
                  ++exported;
               }
               state.addOne();
            }
         } finally {
            buffer.setLength( 0 );
            for ( OutputStreamWriter writer :writers )
               write( "})", writer, buffer );
         }

         listing.write( "])" );
         index.write( "})" );
      }
      if ( exported != category.getExportCount() )
         throw new IllegalStateException( category.id + " entry exported " + category.sorted.length + " mismatch with total " + category.getExportCount() );
   }

   void testViewerExists () throws IOException {
      ResourceUtils.getText( "res/4e_database.html" );
   }

   void writeViewer ( File target ) throws IOException {
      try ( FileOutputStream out = new FileOutputStream( target, false );
            InputStream in = ResourceUtils.getStream( "res/4e_database.html" );
              ) {
        byte[] buffer =  new byte[ 32768 ];
        for ( int length ; (length = in.read( buffer ) ) != -1; )
            out.write( buffer, 0, length );
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private OutputStreamWriter openStream ( String path ) throws FileNotFoundException {
      return new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( path, false ) ), StandardCharsets.UTF_8 );
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

}