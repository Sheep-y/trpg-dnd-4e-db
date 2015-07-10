package db4e.data;

import db4e.Downloader;
import db4e.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Class to load data from local files
 */
public class Loader implements Runnable {

   public static final Logger log = Logger.getLogger( Downloader.class.getName() );

   // For version checkind
   private static final Pattern regx_version = Pattern.compile( "^\\d+," );
   // For data parsing
   private static final Pattern regx_string = Pattern.compile( "\"((?:\\\\.|[^\\\\\"]+)+)\"" );
   private static final Pattern regx_string_array = Pattern.compile( "\\[\\(?:,?" + regx_string.pattern() + "\\)*\\]" );
   private static final Pattern regx_num_field = Pattern.compile( regx_string.pattern() + ":(\\d+)" );

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

   public static JSONArray load ( File f, String jsonp ) {
      assert( jsonp != null );
      try {
         String data = Utils.loadFile( f );
         if ( data == null ) return null;

         data = data.trim();
         if ( data.charAt( 0 ) == '\uFEFF' ) data = data.substring( 1 );

         try {
            if ( ! data.startsWith( jsonp ) || data.length() < jsonp.length()+4 )
               throw new IllegalArgumentException();

            data = '[' + data.substring( jsonp.length() + 1, data.length()-1 ) + ']';

            return (JSONArray) new JSONTokener( data ).nextValue();

         } catch ( IllegalArgumentException | JSONException ex ) {
            log.log( Level.WARNING, "log.malform", f );
            return null;
         }

      } catch ( IOException ex ) {
         log.log( Level.WARNING, "log.cannot_read", f );
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

   private boolean stopped () {
      return Thread.currentThread().isInterrupted();
   }

   private static <E> E[]  toArray( Object obj, IntFunction<E[]> constructor ) {
      JSONArray ary = (JSONArray) obj;
      E[] result = constructor.apply( ary.length() );
      int i = 0;
      for ( Object item : ary )
         result[ i++ ] = (E) item;
      return result;
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public void run () {
      try {
         loadCatalog();
         for ( Category cat : catalog.categories )
            if ( ! stopped() )
               loadCategory( cat );
         log.info( "log.loader.done" );
      } finally {
         catalog.setLoader( null );
         log.info( "log.loader.stopped" );
      }
   }

   private boolean loadCatalog () {
      // Load catalog content: od.reader.jsonp_catalog(20130616,{"Class":19,"Theme":110})
      File f = new File( basefile.getParent(), basefile.getName() + "/catalog.js" );
      log.log( Level.FINE, "log.loader.reading", f );
      JSONArray params = load( f, "od.reader.jsonp_catalog" );
      if ( params == null ) return false;

      // Parse and set catalog content
      Map<String, Integer> data = new HashMap<>();
      try {
         final JSONObject list = (JSONObject) params.get( 1 );
         for ( Object name : list.names() ) {
            String prop = name.toString();
            data.put( prop, list.getInt( prop ) );
         }
      } catch ( ClassCastException | IndexOutOfBoundsException | JSONException ex ) {
         log.log( Level.WARNING, "log.malform", f );
      }

      // Update catalog
      synchronized ( catalog ) {
         if ( stopped() ) return false;
         catalog.addCategories( data.keySet().toArray( new String[0] ) );
         catalog.categories.forEach( cat ->
            cat.size = data.get( cat.id ) );
      }
      log.fine( catalog.toString() );

      return true;
   }

   private boolean loadCategory ( Category cat ) {
      // Entry list: ﻿od.reader.jsonp_data_listing(20130616,"cat_id",["ID","Field2","SourceBook"],[["123.asp","1-2",["Book1","Book2"]],[["234.asp","2-2",["Book3"]]]
      File f = new File( basefile.getParent(), basefile.getName() + "/" + cat.id + "/_listing.js" );
      log.log( Level.FINE, "log.loader.reading", f );
      JSONArray params = load( f, "od.reader.jsonp_data_listing" );
      if ( params == null ) return false;

      // Parse and set catalog content
      try {
         if ( ! cat.id.equals( params.get( 1 ) ) )
            throw new JSONException( "Category ID mismatch" );

         // Load columns
         assert( cat.columns.size() <= 0 );
         synchronized ( cat ) {
            for ( Object col : (JSONArray) params.get( 2 ) )
               cat.columns.add( col.toString() );
         }
         int col_count = cat.columns.size();
         int sourcebook_index = col_count-1;

         // Load listing
         JSONArray items = (JSONArray) params.get( 3 );
         List<Entry> entries = new ArrayList<>( items.length() );
         for ( Object e : items ) {
            JSONArray row = (JSONArray) e;
            String id = row.get( 0 ).toString();
            final Entry entry = new Entry( cat, id, sourcebook_index );
            entries.add( entry );
            for ( int i = 1 ; i < col_count ; i++ ) {
               Object prop = row.get( i );
               if ( prop instanceof JSONArray )
                  entry.columns[ i-1 ] = toArray( row.getJSONArray( i ), String[]::new );
               else
                  entry.columns[ i-1 ] = prop;
            }
         }
         cat.entries.addAll( entries );

         log.fine( cat.toString() );

      } catch ( ClassCastException | IndexOutOfBoundsException | JSONException ex ) {
         log.log( Level.WARNING, "log.malform", f );
      }

      return true;
   }
}