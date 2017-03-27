package db4e.exporter;

import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.IOException;
import java.io.Writer;
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

   @Override public void export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      String root = target.getParent();
      StringBuilder buffer = new StringBuilder( 3 * 1024 * 1024 );

      buffer.append( "Url,Name," );
      for ( String field : category.fields )
         cell( buffer, field ).append( ',' );
      buffer.append( "Content\n" );

      for ( Entry entry : category.entries ) {
         if ( ! entry.downloaded().isContentDownloaded() ) continue;
         cell( buffer.append( entry.getUrl() ).append( ',' ), entry.getName() ).append( ',' );
         for ( String field : entry.getFields() )
            cell( buffer, field ).append( ',' );
         cell( buffer, entry.getContent() ).append( '\n' );
      }
      backspace( buffer );

      if ( stop.get() ) throw new InterruptedException();
      try ( Writer writer = openStream( root + "/" + category.id + ".csv" ) ) {
         writer.write( buffer.toString() );
      }
      state.add( category.entries.size() );
   }

   private StringBuilder cell ( StringBuilder buf, String in ) {
      if ( in.contains( "\"" ) || in.contains( "\n" ) || in.contains( "," ) )
         return buf.append( '"' ).append( in.replace( "\"", "\"\"" ) ).append( '"' );
      return buf.append( in );
   }
}