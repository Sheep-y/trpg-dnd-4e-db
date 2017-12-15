package db4e.exporter;

import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import sheepy.util.ResourceUtils;
import sheepy.util.Utils;

/**
 *\ Export raw data as HTML
 */
public class ExporterRawHtml extends Exporter {

   private String root;

   @Override public synchronized void setState ( File target, Consumer<String> stopChecker, ProgressState state ) {
      super.setState( target, stopChecker, state );
      root = target.toString().replaceAll( "\\.html$", "" ) + "_files/";
   }

   @Override protected void _preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw HTML: {0}", target );
      new File( root ).mkdirs();
      checkStop( "Writing catlog" );
      writeCatalog( categories );
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   private void writeCatalog ( List<Category> categories ) throws IOException {
      final String template = ResourceUtils.getText( "res/export_list.html" );

      final StringBuilder index_body = new StringBuilder();
      final String folder = new File( root ).getName() + "/";

      for ( Category category : categories ) {
         if ( category.entries.isEmpty() ) continue;

         index_body.append( "<tr><td><a href='" ).append( folder ).append( category.id ).append( ".html'>" ).append( Utils.escapeHTML( category.getName() ) ).append( "</a></td>" );
         index_body.append( "<td>" ).append( category.entries.stream().filter( e -> e.hasContent() ).count() ).append( "</td></tr>" );

         final StringBuilder head = new StringBuilder( "<th>Name</th>");
         for ( String field : category.fields )
            head.append( "<th>" ).append( Utils.escapeHTML( field ) ).append( "</th>" );

         final StringBuilder body = new StringBuilder();
         final String cat_id = category.id.toLowerCase() + "/";
         for ( Entry entry : category.entries ) {
            body.append( "<tr><td><a href='" ).append( cat_id ).append( entry.getId().replace( ".aspx?id=", "-" ) ).append( ".html'>" );
            body.append( Utils.escapeHTML( entry.getName() ) ).append( "</a></td>" );
            for ( String field : entry.getSimpleFields() )
               body.append( "<td>" ).append( Utils.escapeHTML( field ) ).append( "</td>" );
            body.append( "</tr>" );
         }

         String output = template;
         output = output.replace( "[title]", Utils.escapeHTML( category.getName() ) );
         output = output.replace( "[head]", head );
         output = output.replace( "[body]", body );
         try ( Writer writer = openStream( root + category.id + ".html" ) ) {
            writer.write( output );
         }
      }

      // Output index
      String output = template;
      output = output.replace( "[title]", "4e Compendium Data" );
      output = output.replace( "[head]", "<th>Category</th><th>Count</th>" );
      output = output.replace( "[body]", index_body );
      try ( Writer writer = openStream( target.toString() ) ) {
         writer.write( output );
      }
   }

   @Override protected void _export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      String template = ResourceUtils.getText( "res/export_entry.html" );
      String cat_id = category.id.toLowerCase();
      new File( root + cat_id ).mkdirs();

      for ( Entry entry : category.entries ) {
         if ( ! entry.hasContent() ) continue;

         if ( stop.get() ) throw new InterruptedException();
         String buffer = template;
         buffer = buffer.replace( "[title]", Utils.escapeHTML( entry.getName() ) );
         buffer = buffer.replace( "[body]", entry.getContent() );
         try ( Writer writer = openStream( root + cat_id + "/" + entry.getId().replace( ".aspx?id=", "-" ) + ".html" ) ) {
            writer.write( buffer );
         }
         state.addOne();
      }
   }
}