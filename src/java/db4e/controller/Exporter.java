package db4e.controller;

import db4e.Main;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

class Exporter {

   private static final Logger log = Main.log;

   private static final Charset utf8 = Charset.forName( "UTF-8" );

   void writeCatalog ( String target, List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 300 );
      try ( OutputStreamWriter writer = openStream( target + "/catalog.js" ) ) {
         buffer.append( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories )
            str( buffer, category.id ).append( ':' ).append( category.total_entry.get() ).append( ',' );
         write( "})", writer, buffer );
      }
   }

   void writeCategory ( String target, Category category, ProgressState state ) throws IOException {
      StringBuilder buffer = new StringBuilder( 1024 );
      File catPath = new File( target + "/" + category.id + "/" );
      catPath.mkdir();

      try ( OutputStreamWriter listing = openStream( catPath + "/_listing.js" );
            OutputStreamWriter   index = openStream( catPath + "/_index.js" )
              ) {

         // List header
         buffer.append( "od.reader.jsonp_data_listing(20130616," );
         str( buffer, category.id ).append( ",[\"ID\",\"Name\"," );
         for ( String header : category.meta )
            str( buffer, header ).append( ',' );
         write( "],[", listing, buffer );

         // Index header
         buffer.append( "od.reader.jsonp_data_index(20130616," );
         str( buffer, category.id );
         write( ",{", index, buffer );

         for ( Entry entry : category.entries ) {
            str( str( buffer.append( '[' ), entry.id ).append( ',' ), entry.name ).append( ',' );
            for ( Object field : entry.meta )
               str( buffer, field.toString() ).append( ',' );
            write( "],", listing, buffer );

            str( str( buffer, entry.id ).append( ':' ), entry.fulltext );
            write( ",", index, buffer );

            state.addOne();
         }

         listing.write( "])" );
         index.write( "})" );
      }

   }

   private OutputStreamWriter openStream ( String path ) throws FileNotFoundException {
      return new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( path, false ) ), utf8 );
   }

   private void write ( CharSequence postfix, Writer writer, StringBuilder buf ) throws IOException {
      if ( ! ( postfix.charAt( 0 ) == ',' ) )
         buf.setLength( buf.length() - 1 ); // Remove last comma
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
