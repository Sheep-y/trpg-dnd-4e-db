package db4e.controller;

import db4e.Main;
import db4e.data.Category;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

class Exporter {

   private static final Logger log = Main.log;

   private static final Charset utf8 = Charset.forName( "UTF-8" );

   void writeCatalog ( String target, List<Category> categories ) throws IOException {
      StringBuilder buffer = new StringBuilder( 20 );
      try ( OutputStreamWriter writer = openStream( target + "/catalog.js" ) ) {
         writer.write( "od.reader.jsonp_catalog(20130616,{" );
         for ( Category category : categories ) {
            buffer.append( '"' ).append( js( category.id ) ).append( "\":" ).append( category.total_entry.get() ).append( ',' );
            writer.write( buffer.toString() );
            buffer.setLength( 0 );
         }
         writer.write( "})" );
      }
   }

   private OutputStreamWriter openStream ( String path ) throws FileNotFoundException {
      return new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( path, false ) ), utf8 );
   }

   private String js ( String in ) {
      return in.replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
   }

}
