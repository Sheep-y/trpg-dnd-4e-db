package db4e.exporter;

import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.log;
import static db4e.exporter.Exporter.stop;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;

/**
 *\ Export raw data as TSV
 */
public class ExporterRawTsv extends Exporter {

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw TSV: {0}", target );
      target.getParentFile().mkdirs();
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public void export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      String root = target.getParent();
      StringBuilder buffer = new StringBuilder( 65536 );

      buffer.append( "Url\tName\t" );
      for ( String field : category.fields )
         cell( buffer, field ).append( '\t' );
      buffer.append( "Content\n" );

      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;
         cell( buffer.append( entry.getUrl() ).append( '\t' ), entry.name ).append( '\t' );
         for ( String field : entry.fields )
            cell( buffer, field ).append( '\t' );
         cell( buffer, entry.content ).append( '\n' );
      }
      backspace( buffer );

      if ( stop.get() ) throw new InterruptedException();
      try ( Writer writer = openStream( root + "/" + category.id + ".tsv" ) ) {
         writer.write( buffer.toString() );
      }
      state.add( category.entries.size() );
   }

   private StringBuilder cell ( StringBuilder buf, String in ) {
      return buf.append( in.replace( "\t", " " ).replace( "\n", " " ) );
   }
}