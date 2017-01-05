package db4e.exporter;

import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;

/**
 *\ Export raw data as Json
 */
public class ExporterRawSql extends Exporter {

   private Writer writer;
   private char id_quote_start;
   private char id_quote_end;
   private char string_prefix;
   private String varchar = " VARCHAR(1000)";
   private String text = " TEXT";

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw Sql: {0}", target );
      target.getParentFile().mkdirs();
      synchronized ( this ) {
         writer = openStream( target.toPath() );
         id_quote_start = id_quote_end = '`';
         string_prefix = ' ';
      }
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public void export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Building {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      StringBuilder buffer = new StringBuilder( 3 * 1024 * 1024 );
      id( buffer.append( "\nDROP TABLE IF EXISTS " ), category.id ).append( ";\n" );
      id( buffer.append( "CREATE TABLE " ), category.id ).append( "(\n  " );
      id( buffer, "Url" ).append( varchar ).append( ",\n  " );
      id( buffer, "Name" ).append( varchar ).append( ",\n  " );
      for ( String field : category.fields )
         id( buffer, field ).append( varchar ).append( ",\n  " );
      id( buffer, "Content" ).append( text );
      buffer.append( ");\n\n" );

      id( buffer.append( "INSERT INTO "), category.id ).append( " VALUES " );

      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;
         buffer.append( "\n(" );
         txt( buffer, entry.getUrl() ).append( ',' );
         txt( buffer, entry.name ).append( ',' );
         for ( String field : entry.fields )
            txt( buffer, field ).append( ',' );
         txt( buffer, entry.content );
         buffer.append( ")," );
      }

      if ( ! buffer.toString().endsWith( " VALUES " ) ) { // If any entry was written
         backspace( buffer ).append( ";\n" );
         synchronized ( this ) {
            writer.write( buffer.toString() );
         }
      }
      state.add( category.entries.size() );
   }

   @Override public synchronized void close() throws IOException {
      if ( writer == null ) return;
      writer.close();
      writer = null;
   }

   private StringBuilder id ( StringBuilder buffer, String id ) {
      return buffer.append( id_quote_start ).append( id ).append( id_quote_end );
   }

   private StringBuilder txt ( StringBuilder buffer, String text ) {
      return buffer.append( string_prefix ).append( '\'' ).append( text.replace( "'", "''" ) ).append( '\'' );
   }
}