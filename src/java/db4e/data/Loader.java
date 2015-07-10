package db4e.data;

import db4e.Downloader;
import db4e.Utils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to load data from local files
 */
public class Loader implements Runnable {

   public static final Logger log = Logger.getLogger( Downloader.class.getName() );

   private static final Pattern regx_version = Pattern.compile( "^\\d+," );
   private static final Pattern regx_catalog = Pattern.compile( "\"((?:\\\\.|[^\\\\\"]+)+)\":(\\d+)" );

   public static void load( Catalog catalog, File basefile ) {
      Thread thread = new Thread( new Loader( catalog, basefile ) );
      thread.setDaemon( true );
      thread.setPriority( Thread.NORM_PRIORITY-1 );
      Runnable aborter = () -> thread.interrupt();
      synchronized ( catalog ) {
         catalog.setLoader( aborter );
         catalog.clear();
      }
      thread.start();
   }

   public static String load ( File f, String jsonp ) {
      try {
         String data = Utils.loadFile( f );
         if ( data == null ) return null;

         data = data.trim();
         if ( data.charAt( 0 ) == '\uFEFF' ) data = data.substring( 1 );
         if ( jsonp != null )
            try {
               if ( ! data.startsWith( jsonp ) || data.length() < jsonp.length()+4 )
                  throw new IllegalArgumentException();

               data = data.substring( jsonp.length() + 1, data.length()-1 );
               Matcher regx = regx_version.matcher( data );
               if ( ! regx.find() )
                  throw new IllegalArgumentException();

               data = regx.replaceFirst( "" ).trim();

            } catch ( IllegalArgumentException ex ) {
               log.log( Level.WARNING, "log.data.err.malform", f );
               return null;
            }

         return data.isEmpty() ? null : data;

      } catch ( IOException ex ) {
         log.log( Level.WARNING, "log.data.err.cannot_read", f );
         return null;
      }
   }

   private final File basefile;
   private final Catalog catalog;

   private Loader ( Catalog catalog, File basefile ) {
      assert( basefile != null && basefile.isFile() );
      this.basefile = basefile;
      this.catalog = catalog;
   }

   private boolean stop() {
      catalog.setLoader( null );
      return true;
   }

   private boolean stopped () {
      if ( Thread.currentThread().isInterrupted() )
         return stop();
      return false;
   }

   public void run () {
      try {
         loadCatalog();
         log.info( "data.loader.done" );
      } finally {
         log.info( "data.loader.stopped" );
      }
   }

   private boolean loadCatalog () {
      // Load catalog content: od.reader.jsonp_catalog(20130616,{"Class":19,"Theme":110})
      File f = new File( basefile.getParent(), basefile.getName() + "/catalog.js" );
      log.log( Level.FINE, "data.loader.reading", f );
      String txt = load( f, "od.reader.jsonp_catalog" );
      if ( txt == null ) return ! stop();

      // Parse and set catalog content
      Map<String, Integer> data = new HashMap<>();
      Matcher regx = regx_catalog.matcher( txt );
      while ( regx.find() )
         data.put( regx.group( 1 ), Integer.parseInt( regx.group( 2 ) ) );

      // Update catalog
      synchronized ( catalog ) {
         if ( stopped() ) return false;
         catalog.addCategories( data.keySet().toArray( new String[0] ) );
         catalog.categories.forEach( cat ->
            cat.size = data.get( cat.id ) );
      }

      return true;
   }
}