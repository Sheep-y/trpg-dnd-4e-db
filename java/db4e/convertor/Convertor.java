package db4e.convertor;

import db4e.Main;
import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
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
   public static AtomicBoolean stop = new AtomicBoolean();
   private static final Map<String, AtomicInteger> fixCount = new HashMap<>();
   private static final Set<String> fixedEntry = new HashSet<>();

   protected final Category category;
   protected final Set<String> corrections = new HashSet<>();

   /**
    * Called before doing any export.
    * Can be used to fix entry count before catalog is saved.
    *
    * @param categories
    */
   public static void beforeConvert ( List<Category> categories, List<Category> exportCategories ) {
      if ( exportCategories.size() > 0 ) return;
      synchronized ( categories ) {
         Map<String,Category> map = new HashMap<>( 18, 1.0f );
         exportCategories.clear();
         if ( map.isEmpty() ) {
            for ( Category c : categories )
               map.put( c.id, c );

            String[] itemMeta =new String[]{ "Type" ,"Level", "Cost", "Rarity", "SourceBook" };
            Category armour    = new Category( "Armor"   , "Armor"    , itemMeta );
            Category implement = new Category( "Implement", "implement", itemMeta );
            Category weapon    = new Category( "Weapon"   , "weapon"   , itemMeta );
            exportCategories.add( armour );
            exportCategories.add( implement );
            exportCategories.add( weapon );

            // This pre-processing does not move progress, and is not MT, and thus should be done very fast.
            for ( Category c : categories ) {
               if ( c.id.equals( "Terrain" ) ) continue;
               Category exported = new Category( c.id, c.name, c.fields );
               exportCategories.add( exported );
               exported.entries.addAll( c.entries );
               switch ( c.id ) {
                  case "Item" :
                     // May convert to parallel stream if this part grows too much...
                     for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
                        Entry entry = i.next();
                        switch ( entry.fields[0] ) {
                           case "Arms":
                              if ( ! entry.content.contains( ">Arms Slot: <" ) || ! entry.content.contains( " shield" ) ) break;
                           case "Armor":
                              i.remove();
                              armour.entries.add( entry );
                              break;
                           case "Implement":
                              i.remove();
                              implement.entries.add( entry );
                              break;
                           case "Weapon":
                              i.remove();
                              if ( entry.content.contains( "<br>Superior <br>" ) )
                                 implement.entries.add( entry );
                              else
                                 weapon.entries.add( entry );
                              break;
                           case "Wondrous":
                              if ( entry.content.contains( "<b>Consumable: </b>Assassin poison" ) ) {
                                 i.remove();
                                 map.get( "Poison" ).entries.add( entry );
                                 // Correction handled by correctEntry
                              }
                        }
                     }
                     break;

                  case "Glossary" :
                     for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
                        Entry entry = i.next();
                        // Various empty glossaries. Such as "male" or "female".  glossary679 "familiar" does not even have published.
                        if ( entry.id.equals( "glossary.aspx?id=679" ) || entry.content.contains( "</h1><p class=\"flavor\"></p><p class=\"publishedIn\">" ) ) {
                           i.remove();
                           corrected( entry, "blacklist" );
                        }
                     }
                     break;

                  case "Trap" :
                     exported.entries.addAll( map.get( "Terrain" ).entries );
               }
            }
         }
      }
   }

   public static void afterConvert () {
      synchronized( fixCount ) {
         log.log( Level.INFO, "Corrected {0} entries: \n{1}", new Object[]{
            fixedEntry.size(),
            fixCount.entrySet().stream()
               .sorted( (a,b) -> b.getValue().get() - a.getValue().get() )
               .map( e -> e.getKey() + " = " + e.getValue().get() ).collect( Collectors.joining( "\n" ) ) } );
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
         case "Armor":
         case "Implement":
         case "Weapon":
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
         if ( stop.get() ) throw new InterruptedException();
         log.log( Level.FINE, "Converting {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         category.meta = category.fields;
         initialise();
         final List<Entry> entries = category.entries;
         for ( Entry entry : entries ) {
            if ( entry.fulltext == null ) {
               convertEntry( entry );
               if ( ! corrections.isEmpty() ) {
                  for ( String fix : corrections )
                     corrected( entry, fix );
                  if ( corrections.size() > 1 )
                     corrected( entry, "multiple fix " + corrections.size() + " (bookkeep)" );
                  corrections.clear();
               }
            }
            if ( stop.get() ) throw new InterruptedException();
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
      correctEntry( entry );
      parseSourceBook( entry );
      entry.fulltext = textData( entry.data );
      // DefaultConvertor will do some checking if debug is on.
   }

   private static void corrected ( Entry entry, String fix ) {
      synchronized ( fixCount ) {
         log.log( Level.FINE, "Corrected {0} {1} ({2})", new Object[]{ entry.shortid, entry.name, fix });
         if ( fixCount.containsKey( fix ) ) fixCount.get( fix ).incrementAndGet();
         else fixCount.put( fix, new AtomicInteger( 1 ) );
         fixedEntry.add( entry.id );
      }
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
   protected abstract void correctEntry ( Entry entry );

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