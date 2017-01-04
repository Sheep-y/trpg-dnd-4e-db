package db4e.exporter;

import db4e.controller.Controller;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;

/**
 *\ Export raw data as CSV
 */
public class ExporterRawCsv extends Exporter {

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw CSV: {0}", target );
      target.getParentFile().mkdirs();
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public Controller.RunExcept export ( Category category ) throws IOException {
      return () -> { synchronized( category ) {
         log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         writeCategory( category );
      } };
   }

   protected void writeCategory ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      String root = target.getParent();
      String cat_id = category.id.toLowerCase();
      new File( root + cat_id ).mkdirs();
      StringBuilder buffer = new StringBuilder( 65536 );

      buffer.append( "Url,Name," );
      for ( String field : category.fields )
         csv( buffer, field ).append( ',' );
      buffer.append( "Content\n" );

      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;
         csv( buffer.append( entry.id ).append( ',' ), entry.name ).append( ',' );
         for ( String field : entry.fields )
            csv( buffer, field ).append( ',' );
         csv( buffer, entry.content ).append( '\n' );
      }
      backspace( buffer );

      if ( stop.get() ) throw new InterruptedException();
      try ( OutputStreamWriter writer = openStream( root + "/" + category.id + ".csv" ) ) {
         write( writer, buffer.toString() );
      }
      state.add( category.entries.size() );
   }

}