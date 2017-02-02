package db4e.exporter;

import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

/**
 *\ Export raw data as Json
 */
public class ExporterRawJson extends Exporter {

   private Writer writer;

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw Json: {0}", target );
      target.getParentFile().mkdirs();
      synchronized ( this ) {
         writer = openStream( target.toPath() );
         writer.write( "{" );
      }
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public void export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Building {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      StringBuilder buffer = new StringBuilder( 3 * 1024 * 1024 );
      str( buffer, category.id ).append( ":[ " ); // extra space to allow backspace in empty category


      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;
         buffer.append( '{' );
         prop( buffer, "Url", entry.getUrl() ).append( ',' );
         prop( buffer, "Name", entry.name ).append( ',' );
         for ( int i = category.fields.length - 1 ; i >= 0 ; i-- )
            prop( buffer, category.fields[ i ], entry.fields[ i ] ).append( ',' );
         prop( buffer, "Content", entry.content );
         buffer.append( "}," );
      }
      backspace( buffer ).append( "]," );

      synchronized ( this ) {
         writer.write( buffer.toString() );
      }
      state.add( category.entries.size() );
   }

   @Override public synchronized void close() throws IOException {
      if ( writer == null ) return;
      // Need to add something to close the last comma.
      writer.write( "\"__date\":\"" + ZonedDateTime.now().format( DateTimeFormatter.ISO_INSTANT ) + "\"}" );
      writer.close();
      writer = null;
   }


   private StringBuilder prop ( StringBuilder buffer, String prop, String value ) {
      return str( str( buffer, prop ).append( ':' ), value );
   }
}