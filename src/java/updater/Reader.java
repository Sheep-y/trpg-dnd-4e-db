package updater;

import db4e.Main;
import db4e.Utils;
import db4e.data.Catalog;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Class to load data from local files
 */
public class Reader implements Runnable {

   public static final Logger log = Logger.getLogger( Main.class.getName() );

   public static Reader load ( Catalog catalog, File basefile ) {
      return new Reader( catalog, basefile );
   }

   public static JSONArray load ( File f, String jsonp ) {
      assert( jsonp != null );
      try {
         String data = Utils.loadFile( f );
         if ( data == null ) return null;

         data = data.trim();
         if ( data.charAt( 0 ) == '\uFEFF' ) data = data.substring( 1 );

         try {
            if ( ! data.startsWith( jsonp ) )
               throw new IllegalArgumentException();

            data = '[' + data.substring( jsonp.length() + 1, data.length()-1 ) + ']';

            return (JSONArray) new JSONTokener( data ).nextValue();

         } catch ( IllegalArgumentException | JSONException | IndexOutOfBoundsException ex ) {
            log.log( Level.WARNING, "log.malform", new Object[]{ f, ex } );
            return null;
         }

      } catch ( IOException ex ) {
         log.log( Level.WARNING, "log.cannot_read", f );
         return null;
      }
   }

   private final File basefile;
   private final Catalog catalog;
   private final Thread thread;

   private Reader ( Catalog catalog, File basepath ) {
      assert( basepath != null && basepath.isFile() );
      this.basefile = basepath;
      this.catalog = catalog;

      Thread thread = new Thread( this, "Reader" );
      this.thread = thread;
      thread.setDaemon( false );
//      thread.setPriority( Thread.NORM_PRIORITY-1 );
   }

   public void start () {
      thread.start();
   }

   public void stop () {
      thread.interrupt();
   }

   public boolean isInterrupted () {
      return thread.isInterrupted();
   }

   private static <E> E[] toArray( Object obj, IntFunction<E[]> constructor ) {
      JSONArray ary = (JSONArray) obj;
      E[] result = constructor.apply( ary.length() );
      int i = 0;
      for ( Object item : ary )
         result[ i++ ] = (E) item;
      return result;
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private final BooleanProperty running = new SimpleBooleanProperty();
   public final ReadOnlyBooleanProperty isRunning = ReadOnlyBooleanProperty.readOnlyBooleanProperty( running );

   @Override public void run () {
      running.set( true );
      try {
         loadCatalog();
         for ( Category cat : catalog.categories ) {
            if ( isInterrupted() ) return;
            loadCategory( cat );
         }
         for ( Category cat : catalog.categories ) {
            for ( Entry entry : cat.entries ) {
               if ( isInterrupted() ) return;
               loadEntry( entry );
            }
            log.log( Level.FINE, "log.reader.entry", new Object[]{ cat.id, cat.entries.size() } );
         }
         log.info( "log.reader.done" );
      } finally {
         log.info( "log.reader.stopped" );
         running.set( false );
      }
   }

   private void loadCatalog () {
      // Load catalog content: od.reader.jsonp_catalog(20130616,{"Class":19,"Theme":110})
      File f = new File( basefile, "catalog.js" );
      log.log( Level.FINE, "log.reader.reading", f );
      JSONArray params = load( f, "od.reader.jsonp_catalog" );
      if ( params == null ) return;

      // Parse and set catalog content
      Map<String, Integer> data = new HashMap<>();
      try {
         final JSONObject list = (JSONObject) params.get( 1 );
         for ( Object name : list.names() ) {
            String prop = name.toString();
            data.put( prop, list.getInt( prop ) );
         }
      } catch ( ClassCastException | IndexOutOfBoundsException | JSONException ex ) {
         log.log( Level.WARNING, "log.malform", new Object[]{ f, ex } );
      }

      // Update catalog
      synchronized ( catalog ) {
         if ( isInterrupted() ) return;
         catalog.addCategories( data.keySet().toArray( new String[0] ) );
         catalog.categories.forEach( cat ->
            cat.size = data.get( cat.id ) );
      }
      log.fine( catalog.toString() );
   }

   private void loadCategory ( Category cat ) {
      // Entry list: ﻿od.reader.jsonp_data_listing(20130616,"cat_id",["ID","Field2","SourceBook"],[["123.asp","1-2",["Book1","Book2"]],[["234.asp","2-2",["Book3"]]]
      File f = new File( basefile, cat.id + "/_listing.js" );
      log.log( Level.FINE, "log.reader.reading", f );
      JSONArray params = load( f, "od.reader.jsonp_data_listing" );
      if ( params == null ) return;

      // Parse and set catalog columns and listing
      try {
         if ( ! cat.id.equals( params.get( 1 ) ) )
            throw new JSONException( "Category ID mismatch" );

         // Load columns
         assert( cat.meta.size() <= 0 );
         synchronized ( cat ) {
            for ( Object e : (JSONArray) params.get( 2 ) )
               cat.meta.add( e.toString() );
         }
         if ( ! cat.meta.contains( "ID" ) ) cat.meta.add( "ID" );
         int meta_count = cat.meta.size();
         String[] meta = cat.meta.toArray( new String[ meta_count ] );

         // Load listing
         JSONArray items = (JSONArray) params.get( 3 );
         List<Entry> entries = new ArrayList<>( items.length() );
         for ( Object e : items ) {
            JSONArray row = (JSONArray) e;
            String id = row.get( 0 ).toString();
            final Entry entry = new Entry( cat, id );
            for ( int i = 1 ; i < meta_count ; i++ ) {
               Object prop = row.get( i );
               if ( prop instanceof JSONArray )
                  entry.setMeta( meta[i], toArray( row.getJSONArray( i ), String[]::new ) );
               else
                  entry.setMeta( meta[i], prop );
            }
            entries.add( entry );
         }
         synchronized ( cat ) {
            cat.entries.addAll( entries );
         }

         log.fine( cat.toString() );

      } catch ( ClassCastException | IndexOutOfBoundsException | JSONException ex ) {
         log.log( Level.WARNING, "log.malform", new Object[]{ f, ex } );
      }
   }

   private void loadEntry ( Entry entry ) {
      //﻿od.reader.jsonp_data(20130330, "Cat", "id","content" )
      File f = new File( basefile, entry.category.id + "/" + entry.getFileId() + ".js" );
      log.log( Level.FINER, "log.reader.reading", f );
      JSONArray params = load( f, "od.reader.jsonp_data" );
      if ( params == null ) return;

      // Parse and set entry content
      try {
         if ( ! entry.category.id.equals( params.get( 1 ) ) || ! entry.getId().equals( params.getString( 2 ) ) )
            throw new JSONException( "Entry ID or Category mismatch" );

         synchronized ( entry ) {
            entry.content = params.getString( 3 );
         }

      } catch ( IndexOutOfBoundsException | JSONException ex ) {
         log.log( Level.WARNING, "log.malform", new Object[]{ f, ex } );
      }

   }
}