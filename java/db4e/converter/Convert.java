package db4e.converter;

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
 * This class contains high level skeleton code; Converter has fine implementations.
 */
public abstract class Convert {

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
                     transferItem( exported, armour, implement, weapon, map );
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

   private static void transferItem( Category exported, Category armour, Category implement, Category weapon, Map<String, Category> map ) {
      // May convert to parallel stream if this part grows too much...
      for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
         Entry entry = i.next();
         switch ( entry.fields[0] ) {
            case "Arms":
               if ( ! entry.content.contains( ">Arms Slot: <" ) || ! entry.content.contains( " shield" ) ) break;
               // falls through
            case "Armor":
               i.remove();
               armour.entries.add( entry );
               break;
            case "Consumable":
               if ( entry.content.contains( "<b>Consumable: </b>Assassin poison" ) ) {
                  i.remove();
                  map.get( "Poison" ).entries.add( entry );
                  // Correction handled by correctEntry
               }
               break;
            case "Equipment":
               switch ( entry.name ) {
                  case "Arrow": case "Arrows":
                  case "Crossbow Bolt": case "Crossbow Bolts":
                  case "Sling Bullet": case "Sling Bullets":
                  case "Blowgun Needles": case "Magazine":
                     i.remove();
                     weapon.entries.add( entry );
                     break;
                  case "Holy Symbol":
                  case "Ki Focus":
                     i.remove();
                     implement.entries.add( entry );
                     break;
                  default:
                     if ( entry.name.endsWith( "Implement" ) ) {
                        i.remove();
                        implement.entries.add( entry );
                     }
               }
               break;
            case "Implement":
               i.remove();
               implement.entries.add( entry );
               break;
            case "Ammunition":
            case "Weapon":
               i.remove();
               if ( entry.content.contains( "<br>Superior <br>" ) )
                  implement.entries.add( entry );
               else
                  weapon.entries.add( entry );
               break;
            case "Artifact":
               switch ( entry.id ) {
                  case "item.aspx?id=105": // Shield of Prator
                     moveArtifact( i, armour, entry, "Heavy shield" ); break;
                  case "item.aspx?id=117": // The Invulnerable Coat of Arnd
                     moveArtifact( i, armour, entry, "Chain, scale or plate" ); break;
                  case "item.aspx?id=139": // Plastron of Tziphal
                     moveArtifact( i, armour, entry, "Plate" ); break;

                  case "item.aspx?id=114": // Axe of the Dwarvish Lords
                     moveArtifact( i, weapon, entry, "Greataxe" ); break;
                  case "item.aspx?id=108": // Broken Blade of Banatruul
                  case "item.aspx?id=125": // Soul Sword
                  case "item.aspx?id=142": // Ruinblade
                     moveArtifact( i, weapon, entry, "Greatsword" ); break;
                  case "item.aspx?id=107": // Spear of the Skylord
                  case "item.aspx?id=119": // Spear of Urrok the Brave
                     moveArtifact( i, weapon, entry, "Longspear" ); break;
                  case "item.aspx?id=128": // Von Zarovich Family Sword
                  case "item.aspx?id=152": // Nightbringer
                  case "item.aspx?id=162": // Justice's Edge
                     moveArtifact( i, weapon, entry, "Longsword" ); break;
                  case "item.aspx?id=158": // Wand of Orcus
                     moveArtifact( i, weapon, entry, "Mace" ); break;
                  case "item.aspx?id=126": // Sword of Kas
                     moveArtifact( i, weapon, entry, "Short sword" ); break;
                  case "item.aspx?id=150": // Heartwood Spear
                     moveArtifact( i, weapon, entry, "Spear" ); break;
                  case "item.aspx?id=141": // Wave
                     moveArtifact( i, weapon, entry, "Trident" ); break;
                  case "item.aspx?id=102": // Hammer of Thunderbolts
                  case "item.aspx?id=129": // Whelm
                     moveArtifact( i, weapon, entry, "Warhammer" ); break;

                  case "item.aspx?id=145": // The Deluvian Hourglass
                     moveArtifact( i, implement, entry, null ); break;
                  case "item.aspx?id=140": // Crystal of Ebon Flame
                     moveArtifact( i, implement, entry, "Any" ); break;
                  case "item.aspx?id=123": // Orb of Light
                  case "item.aspx?id=132": // Cup and Talisman of Al'Akbar
                  case "item.aspx?id=138": // Seal of the Lawbringer
                     moveArtifact( i, implement, entry, "Holy Symbol" ); break;
                  case "item.aspx?id=113": // Blue Orb of Dragonkind
                  case "item.aspx?id=127": // Tome of Shadow
                  case "item.aspx?id=148": // Faarlung's Algorithm
                  case "item.aspx?id=149": // Dreamheart
                  case "item.aspx?id=151": // Orb of Kalid-Ma
                  case "item.aspx?id=154": // Skull of Sartine
                     moveArtifact( i, implement, entry, "Orb" ); break;
                  case "item.aspx?id=135": // Rod of Seven Parts
                     moveArtifact( i, implement, entry, "Rod" ); break;
                  case "item.aspx?id=100": // Book of Infinite Spells
                  case "item.aspx?id=143": // The Deck of Many Things (Heroic)
                  case "item.aspx?id=144": // The Deck of Many Things (Paragon)
                  case "item.aspx?id=160": // Book of Vile Darkness
                     moveArtifact( i, implement, entry, "Tome" ); break;
                  case "item.aspx?id=104": // The Shadowstaff
                  case "item.aspx?id=156": // Audaviator
                     moveArtifact( i, implement, entry, "Staff" ); break;
                  case "item.aspx?id=147": // Arrow of Fate
                     Entry copy = entry.clone();
                     moveArtifact( null, weapon, entry, "Spear or arrow" );
                     entry = copy;
                     // fall through
                  case "item.aspx?id=164": // Staff of Fraz-Urb'luu
                     moveArtifact( i, implement, entry, "Rod, staff or wand" ); break;
                  case "item.aspx?id=146": // Seed of Winter
                     moveArtifact( i, implement, entry, "Wand or totem" ); break;

                  case "item.aspx?id=110": // Figurine of Tantron
                  case "item.aspx?id=130": // Adamantine Horse of Xarn
                     markArtifact( entry, "Wondrous" );
                     entry.meta[1] = "Artifact: Steed";
                     break;
                  case "item.aspx?id=134": // Rash and Reckless
                     markArtifact( entry, "Feet" ); break;
                  case "item.aspx?id=116": // The Hand of Vecna
                     markArtifact( entry, "Hands" ); break;
                  case "item.aspx?id=111": // Helm of the Madman’s Blood
                  case "item.aspx?id=115": // The Eye of Vecna
                  case "item.aspx?id=121": // Jet Black Ioun Stone
                  case "item.aspx?id=124": // Silver Mask of Kas
                  case "item.aspx?id=155": // Eye of the Old Gods
                  case "item.aspx?id=157": // The Ashen Crown
                  case "item.aspx?id=159": // Crown of Dust (First Fragment)
                  case "item.aspx?id=161": // Xraunran Crown of Eyes
                  case "item.aspx?id=163": // Crown of Whispers (Dragon 413)
                     markArtifact( entry, "Head" ); break;
                  case "item.aspx?id=165": // Chromodactylic Loom
                     markArtifact( entry, "Lair" ); break;
                  case "item.aspx?id=103": // Jacinth of Inestimable Beauty
                  case "item.aspx?id=106": // Zax, Cloak of Kings
                  case "item.aspx?id=118": // Ilthuviel's Blackened Heart
                  case "item.aspx?id=131": // Amulet of Passage
                     markArtifact( entry, "Neck" ); break;
                  case "item.aspx?id=120": // Unconquered Standard of Arkhosia
                  case "item.aspx?id=136": // Standard of Eternal Battle
                     markArtifact( entry, "Standard" ); break;
                  case "item.aspx?id=101": // Codex of Infinite Planes
                  case "item.aspx?id=109": // The Immortal Game
                  case "item.aspx?id=112": // Wayfinder Badge
                  case "item.aspx?id=122": // Mirror of Secrets
                  case "item.aspx?id=133": // Emblem of Ossandrya
                  case "item.aspx?id=137": // Blood of Io
                  case "item.aspx?id=153": // Head of Vyrellis
                     markArtifact( entry, "Wondrous" ); break;
               }
         }
      }
   }

   private static void moveArtifact ( Iterator<Entry> i, Category target, Entry entry, String type ) {
      if ( i != null ) i.remove();
      target.entries.add( entry );
      String[] fields = entry.fields;
      type = type == null ? "" : ": " + type;
      entry.meta = new Object[]{ "Artifact" + type, fields[1], fields[2], fields[3], fields[4] };
   }

   private static void markArtifact ( Entry entry, String type ) {
      String[] fields = entry.fields;
      entry.meta = new Object[]{ type, "Artifact", fields[1], fields[2], fields[3], fields[4] };
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

   public static Converter getConverter ( Category category, boolean debug ) {
      switch ( category.id ) {
         case "Ritual":
         case "Monster":
         case "Poison":
         case "Disease":
            return new LeveledConverter( category, debug );
         case "Companion":
         case "Glossary":
            return new FieldSortConverter( category, 0, debug ); // Sort by first field
         case "Trap":
            return new TrapConverter( category, debug );
         case "Feat":
            return new FeatConverter( category, debug );
         case "Item":
         case "Armor":
         case "Implement":
         case "Weapon":
            return new ItemConverter( category, debug );
         case "Power":
            return new PowerConverter( category, debug );
         case "Terrain":
            return null;
         default:
            return new Converter( category, debug );
      }
   }

   protected Convert ( Category category ) {
      this.category = category;
   }

   public CompletableFuture<Void> convert ( ProgressState state, Executor pool ) {
      final CompletableFuture<Void> result = new CompletableFuture();
      pool.execute( () -> { try { synchronized ( category ) {
         if ( stop.get() ) throw new InterruptedException();
         log.log( Level.FINE, "Converting {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
         if ( category.meta == null )
            initialise();
         final List<Entry> entries = category.entries;
         for ( Entry entry : entries ) {
            if ( entry.fulltext == null ) try {
               convertEntry( entry );
               if ( ! corrections.isEmpty() ) {
                  for ( String fix : corrections )
                     corrected( entry, fix );
                  if ( corrections.size() > 1 )
                     corrected( entry, "multiple fix " + corrections.size() + " (bookkeep)" );
                  corrections.clear();
               }
            } catch ( Exception e ) {
               throw new UnsupportedOperationException( "Error converting " + entry.shortid, e );
            }
            if ( stop.get() ) throw new InterruptedException();
            state.addOne();
         }
         if ( category.sorted == null ) {
            beforeSort();
            category.sorted = entries.toArray( new Entry[ entries.size() ] );
            Arrays.sort( category.sorted, this::sortEntity );
         }
         result.complete( null );
      } } catch ( Exception e ) {
         result.completeExceptionally( e );
      } } );
      return result;
   }

   /**
    * Called at the beginning of entity conversion.  Will be called in every export.
    */
   protected void initialise()  {
      if ( category.meta == null )
         category.meta = category.fields;
   }

   /**
    * Called at the end of entity conversion but before sort.  Will be called in every export.
    */
   protected void beforeSort()  { }

   protected int sortEntity ( Entry a, Entry b ) {
      return a.name.compareTo( b.name );
   }

   /**
    * Apply common conversions to entry data.
    * entry.meta may be set, but other prorerties will be overwritten.
    *
    * @param entry Entry to be converted
    */
   protected void convertEntry ( Entry entry ) {
      entry.display_name = entry.name.replace( "’", "'" );
      entry.shortid = entry.id.replace( ".aspx?id=", "" ).toLowerCase();
      if ( entry.meta == null ) {
         final int length = entry.fields.length;
         entry.meta = new Object[ length ];
         System.arraycopy( entry.fields, 0, entry.meta, 0, length );
      }
      entry.data = normaliseData( entry.content );
      correctEntry( entry );
      parseSourceBook( entry );
      entry.fulltext = textData( entry.data );
      // Converter will do some checking if debug is on.
   }

   /**
    * Log correction and keep track of correction count.
    * @param entry Fixed entry.
    * @param fix Type of fix.
    */
   private static void corrected ( Entry entry, String fix ) {
      synchronized ( fixCount ) {
         log.log( Level.FINE, "Corrected {0} {1} ({2})", new Object[]{ entry.shortid, entry.name, fix });
         if ( fixCount.containsKey( fix ) ) fixCount.get( fix ).incrementAndGet();
         else fixCount.put( fix, new AtomicInteger( 1 ) );
         fixedEntry.add( entry.id );
      }
   }

   /**
    * Entry specific data fixes. No need to call super when overriden.
    * @param entry Entry to be corrected.
    */
   protected abstract void correctEntry ( Entry entry );

   /**
    * Remove / convert images, unicode, and redundent whitespace
    * @param data Text data to normalise
    * @return Normalided data
    */
   protected abstract String normaliseData ( String data );

   /**
    * Read the sourcebook meta data and convert to abbreviated form.
    * @param entry
    */
   protected abstract void parseSourceBook ( Entry entry );

   /**
    * Convert HTML data into full text data for full text search.
    *
    * @param data Data to strip
    * @return Text data
    */
   protected abstract String textData ( String data );
}