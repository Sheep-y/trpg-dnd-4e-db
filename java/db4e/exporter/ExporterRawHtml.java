package db4e.exporter;

import db4e.controller.Controller;
import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import sheepy.util.ResourceUtils;
import sheepy.util.Utils;

/**
 *\ Export raw data as HTML
 */
public class ExporterRawHtml extends Exporter {

   private final String root;

   public ExporterRawHtml ( File target, Consumer<String> stopChecker, ProgressState state ) {
      super( target, stopChecker, state );
      root = target.toString().replaceAll( "\\.html$", "" ) + "_files/";
   }

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export target: {0}", target );
      try {
         testRawViewerExists();
      } catch ( IOException ex ) {
         throw new FileNotFoundException( "No viewer. Run ant make-viewer." );
      }
      log.log( Level.CONFIG, "Export raw root: {0}", target );
      new File( root ).mkdirs();
      checkStop( "Writing catlog" );
      writeRawCatalog( root, categories, target );
      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public Controller.RunExcept export ( Category category ) throws IOException {
      return () -> { synchronized( category ) {
         log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         writeRawCategory( root, category, state );
      } };
   }

   @Override public void postExport ( List<Category> categories ) throws IOException {
      log.log( Level.INFO, "Exported {0} entries in {1} catrgories.", new Object[]{
         state.total,
         categories.size() } );
   }

   protected void writeRawCatalog ( String root, List<Category> categories, File target ) throws IOException {
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

   protected void writeRawCategory ( String root, Category category, ProgressState state ) throws IOException, InterruptedException {
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

   protected void testRawViewerExists () throws IOException {
      ResourceUtils.getText( "res/export_list.html" );
      ResourceUtils.getText( "res/export_entry.html" );
   }
}