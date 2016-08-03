package db4e.convertor;

import db4e.Main;
import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Convert category and entry data for export.
 * This class contains high level skeleton code; DefaulfConvertor has fine implementations.
 */
public abstract class Convertor {

   protected static final Logger log = Main.log;
   protected static final Map<String, AtomicInteger> corrected = new HashMap<>();
   public static volatile boolean stop = false;

   protected final Category category;
   protected static final Map<String,Category> categories = new HashMap<>( 18, 1.0f );

   /**
    * Called before doing any export.
    * Can be used to fix entry count before catalog is saved.
    *
    * @param categories
    */
   public static void beforeConvert ( List<Category> categories ) {
      synchronized ( Convertor.categories ) {
         if ( Convertor.categories.isEmpty() ) {
            for ( Category c : categories )
               Convertor.categories.put( c.id, c );
            int terrainCount = Convertor.categories.get( "Terrain" ).total_entry.get();
            Convertor.categories.get( "Terrain" ).exported_entry_deviation.set( -terrainCount );
            Convertor.categories.get( "Trap" ).exported_entry_deviation.set( terrainCount );
            Convertor.categories.get( "Glossary" ).exported_entry_deviation.set( -79 );
         }
      }
      synchronized( corrected ) {
         corrected.clear();
      }
   }

   public static void afterConvert () {
      synchronized( corrected ) {
         log.log( Level.INFO, "Corrected {0} entries: \n{1}", new Object[]{
            corrected.values().stream().mapToInt( e -> e.get() ).sum(),
            corrected.entrySet().stream().map( e -> e.getKey() + " = " + e.getValue().get() ).collect( Collectors.joining( "\n" ) ) } );
      }
   }

   protected Convertor ( Category category ) {
      this.category = category;
   }

   public static Convertor getConvertor ( Category category, boolean debug ) {
      switch ( category.id ) {
         case "Ritual":
         case "Monster":
         case "Poison":
         case "Disease":
            return new LeveledConvertor( category, debug );
         case "Companion":
         case "Glossary":
            return new FieldSortConvertor( category, 0, debug ); // Sort by first field
         case "Trap":
            return new TrapConvertor( category, debug );
         case "Feat":
            return new FeatConvertor( category, debug );
         case "Item":
            return new ItemConvertor( category, debug );
         case "Power":
            return new PowerConvertor( category, debug );
         case "Terrain":
            return null;
         default:
            return new DefaultConvertor( category, debug );
      }
   }

   public CompletableFuture<Void> convert ( ProgressState state, Executor pool ) {
      final CompletableFuture<Void> result = new CompletableFuture();
      pool.execute( () -> { try { synchronized ( category ) {
         if ( stop ) throw new InterruptedException();
         log.log( Level.FINE, "Converting {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         if ( category.meta == null )
            category.meta = category.fields;
         initialise();
         final List<Entry> entries = getExportEntries();
         for ( Entry entry : entries ) {
            if ( entry.content == null ) throw new IllegalStateException( entry.name + " (" + category.name + ") has no content" );
            convertEntry( entry );
            if ( stop ) throw new InterruptedException();
            state.addOne();
         }
         if ( category.sorted == null ) {
            category.sorted = entries.toArray( new Entry[ entries.size() ] );
            Arrays.sort( category.sorted, this::sortEntity );
         }
         result.complete( null );
      } } catch ( Exception e ) {
         result.completeExceptionally( e );
      } } );
      return result;
   }

   protected void initialise()  { }

   protected List<Entry> getExportEntries() {
      return category.entries;
   }

   protected int sortEntity ( Entry a, Entry b ) {
      return a.name.compareTo( b.name );
   }

   /**
    * Apply common conversions to entry data.
    * entry.meta may be set, but other prorerties will be overwritten.
    *
    * @param entry
    */
   protected void convertEntry ( Entry entry ) {
      entry.display_name = entry.name.replace( "’", "'" );
      entry.shortid = entry.id.replace( ".aspx?id=", "" ).toLowerCase();
      copyMeta( entry );
      entry.data = normaliseData( entry.content );
      String fixApplied = correctEntry( entry );
      if ( fixApplied != null ) synchronized ( corrected ) {
         log.log( Level.FINE, "Corrected {0} {1} ({2})", new Object[]{ entry.shortid, entry.name, fixApplied });
         if ( corrected.containsKey( fixApplied ) ) corrected.get( fixApplied ).incrementAndGet();
         else corrected.put( fixApplied, new AtomicInteger( 1 ) );
      }
      if ( "null".equals( entry.shortid ) ) return;
      parseSourceBook( entry );
      entry.fulltext = textData( entry.data );
      // DefaultConvertor will do some checking if debug is on.
   }

   protected void copyMeta ( Entry entry ) {
      if ( entry.meta != null ) return;
      final int length = entry.fields.length;
      entry.meta = new Object[ length ];
      System.arraycopy( entry.fields, 0, entry.meta, 0, length );
   }

   /**
    * Entry specific data fixes. No need to call super when overriden.
    * @return The kind of fix done for this entry. Or null if already correct.
    */
   protected abstract String correctEntry ( Entry entry );

   /**
    * Remove / convert images, unicode, and redundent whitespace
    * @param data Text data to normalise
    * @return Normalided data
    */
   protected abstract String normaliseData ( String data );

   protected abstract void parseSourceBook ( Entry entry );

   /**
    * Convert HTML data into full text data for full text search.
    *
    * @param data Data to strip
    * @return Text data
    */
   protected abstract String textData ( String data );
}