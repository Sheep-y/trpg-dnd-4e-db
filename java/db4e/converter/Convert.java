package db4e.converter;

import db4e.Main;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import sheepy.util.Utils;
import static sheepy.util.Utils.sync;

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

   protected Entry entry; // Current convert subject

   public static void reset () {
   }

   /**
    * Map original data categories to export (fixed) categories.
    *
    * After this, each export category will be processed in its own thread, so
    * all entries must be assigned to the final export categories here.
    *
    * @param categories Original compendium categories.
    * @return Mapped export categories
    */
   public static List<Category> mapExportCategories ( List<Category> categories ) {
      final int EXPORT_CAT_COUNT = 20;
      final List<Category> result = new ArrayList<>( EXPORT_CAT_COUNT );
      final Map<String,Category> map = new HashMap<>( 20, 1f ); // A temp dictionary to quickly find cat by id

      final String[] itemMeta  = new String[]{ "Type" ,"Level", "Cost", "Rarity", "SourceBook" };
      final Category armour    = new Category( "Armor"    , "Armor"    , itemMeta );
      final Category implement = new Category( "Implement", "implement", itemMeta );
      final Category weapon    = new Category( "Weapon"   , "weapon"   , itemMeta );
      result.add( armour );
      result.add( implement );
      result.add( weapon );

      for ( Category c : sync( categories ) )
         if ( ! c.id.equals( "Terrain" ) ) // Terrain is merged into Traps
            result.add( new Category( c.id, c.name, c.fields ) );

      for ( Category c : result ) // Create export map
         map.put( c.id, c );

      // Move entries around before export
      for ( Category source : sync( categories ) ) synchronized( source ) {
         String exportTarget = source.id.equals( "Terrain" ) ? "Trap" : source.id; // Moves terrain into trap.
         Category exported = map.get( exportTarget );
         synchronized( exported ) {
            source.entries.stream().forEach( e -> exported.entries.add( e.clone() ) );
            switch ( source.id ) {
               case "Glossary" :
                  for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
                     Entry entry = i.next();
                     // Various empty glossaries. Such as "male" or "female".  glossary679 "familiar" does not even have published.
                     if ( entry.getId().equals( "glossary.aspx?id=679" ) || entry.getContent().contains( "</h1><p class=\"flavor\"></p><p class=\"publishedIn\">" ) ) {
                        i.remove();
                        corrected( entry, "blacklist" );
                     }
                  }
                  exported.entries.add( new Entry().setId( "glossary0453" ).setName( "Item Set" ) );
                  break;

               case "Item" :
                  transferItem( exported, map );
                  break;

               case "Background" :
                  for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
                     Entry entry = i.next();
                     // Nine backgrounds from Dra376 are hooks only, not actual character resources.
                     if ( entry.getField( 3 ).toString().endsWith( "376" ) ) {
                        switch ( entry.getId() ) {
                           case "background.aspx?id=283" : case "background.aspx?id=284" : case "background.aspx?id=285" :
                           case "background.aspx?id=286" : case "background.aspx?id=287" : case "background.aspx?id=288" :
                           case "background.aspx?id=289" : case "background.aspx?id=290" : case "background.aspx?id=291" :
                              i.remove();
                              corrected( entry, "blacklist" );
                        }
                     }
                  }
                  break;
            }
         }
      }

      // Export big categories first to better balance CPU workload, and to die early on out of memory
      synchronized( result ) { result.sort( ( a, b ) -> b.getExportCount() - a.getExportCount() ); }
      if ( Main.debug.get() && result.size() != EXPORT_CAT_COUNT )
         log.log( Level.WARNING, "Export category map has incorrect size {0}. {1} expected.", new Object[]{ result.size(), EXPORT_CAT_COUNT });
      return result;
   }

   private static void transferItem ( Category exported, Map<String, Category> map ) {
      // May convert to parallel stream if this part grows too much...
      final Category armour = map.get( "Armor" );
      final Category implement = map.get( "Implement" );
      final Category weapon = map.get( "Weapon" );

      for ( Iterator<Entry> i = exported.entries.iterator() ; i.hasNext() ; ) {
         Entry entry = i.next();
         synchronized ( entry ) {
         switch ( entry.getField( 0 ).toString() ) {
            case "Arms":
               if ( ! entry.getContent().contains( ">Arms Slot: <" ) || ! entry.getContent().contains( " shield" ) ) break;
               // falls through
            case "Armor":
               i.remove();
               armour.entries.add( entry );
               break;
            case "Consumable":
               if ( entry.getContent().contains( "<b>Consumable: </b>Assassin poison" ) ) {
                  i.remove();
                  synchronized ( map.get( "Poison" ) ) {
                     map.get( "Poison" ).entries.add( entry ); // Meta correction and fix registration handled by correctEntry
                  }
               }
               break;
            case "Equipment":
               switch ( entry.getName() ) {
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
                     if ( entry.getName().endsWith( "Implement" ) ) {
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
               if ( entry.getContent().contains( "<br>Superior <br>" ) )
                  implement.entries.add( entry );
               else
                  weapon.entries.add( entry );
               break;
            case "Artifact":
               switch ( entry.getId() ) {
                  // Armours
                  case "item.aspx?id=105": // Shield of Prator
                     moveArtifact( i, armour, entry, "Heavy shield" ); break;
                  case "item.aspx?id=117": // The Invulnerable Coat of Arnd
                     moveArtifact( i, armour, entry, "Chain, scale or plate" ); break;
                  case "item.aspx?id=139": // Plastron of Tziphal
                     moveArtifact( i, armour, entry, "Plate" ); break;

                  // Weapons
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

                  // Implements
                  case "item.aspx?id=145": // The Deluvian Hourglass
                     moveArtifact( i, implement, entry, "Any" ); break;
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
                  case "item.aspx?id=147": // Arrow of Fate - make a copy for weapon and then move to implement
                     moveArtifact( null, weapon, entry.clone(), "Spear, arrow or bolt" );
                     moveArtifact( i, implement, entry, "Rod, staff or wand" );
                     break;
                  case "item.aspx?id=164": // Staff of Fraz-Urb'luu
                     moveArtifact( i, implement, entry, "Rod, staff or wand" ); break;
                  case "item.aspx?id=146": // Seed of Winter
                     moveArtifact( i, implement, entry, "Wand or totem" ); break;

                  // General Equipments
                  case "item.aspx?id=110": // Figurine of Tantron
                  case "item.aspx?id=130": // Adamantine Horse of Xarn
                     markArtifact( entry, "Wondrous" );
                     entry.setField( 1, "Mount" );
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
                     markArtifact( entry, "Wondrous" );
                     entry.setField( 1, "Standard" );
                     break;
                  case "item.aspx?id=101": // Codex of Infinite Planes
                  case "item.aspx?id=109": // The Immortal Game
                  case "item.aspx?id=112": // Wayfinder Badge
                  case "item.aspx?id=122": // Mirror of Secrets
                  case "item.aspx?id=133": // Emblem of Ossandrya
                  case "item.aspx?id=137": // Blood of Io
                  case "item.aspx?id=153": // Head of Vyrellis
                     markArtifact( entry, "Wondrous" ); break;
               }
         } }
      }
   }

   private static void moveArtifact ( Iterator<Entry> i, Category target, Entry entry, String type ) { synchronized ( entry ) {
      if ( i != null ) i.remove();
      target.entries.add( entry );
      Object[] fields = entry.getFields();
      entry.setFields( type, fields[1], fields[2], "Artifact", fields[4] );
   } }

   private static void markArtifact ( Entry entry, String category ) { synchronized ( entry ) {
      Object[] fields = entry.getFields();
      entry.setFields( category, "", fields[1], fields[2], "Artifact", fields[4] );
   } }

   public static void afterConvert () {
      synchronized( fixCount ) {
         log.log( Level.INFO, "Corrected {0} entries: \n{1}", new Object[]{
            fixedEntry.size(),
            fixCount.entrySet().stream()
               .sorted( (a,b) -> b.getValue().get() - a.getValue().get() )
               .map( e -> e.getKey() + " = " + e.getValue().get() ).collect( Collectors.joining( "\n" ) ) } );
         fixCount.clear();
         fixedEntry.clear();
      }
   }

   /** Populate category.index with lookup index. (name => id) */
   public void mapIndex () {
      Map<String, List<String>> map = category.index = new HashMap<>( 25000, 1f );
      Set<String> lookups = new HashSet<>();
      for ( Entry entry : sync( category.entries, category ) ) synchronized ( entry ) { // Raw export may skip convert, so we still need to sync!
         this.entry = entry;
         for ( String name : getLookupName( entry, lookups ) ) {
            name = name.replaceAll( "[^\\w'-éû]+", " " ).trim().toLowerCase();
            if ( ! map.containsKey( name ) ) {
               List<String> idList = new ArrayList<>( 2 ); // Most lookup names do not have multiple entries
               idList.add( entry.getId() );
               map.put( name, idList );
            } else
               map.get( name ).add( entry.getId() );
         }
         lookups.clear();
      }

      if ( Main.debug.get() ) try {
         // Check short index (1 or 2 characters).  Only exception is hp and "Og, Orog Hero" (monster1132)
         category.index.keySet().stream().filter( key -> key.length() <= 2 && ! key.equals( "hp" ) && ! key.equals( "og" ) )
            .forEach( key -> category.index.get( key ).forEach( id ->
               log.log( Level.WARNING, "Short index \"{2}\": {0} {1}", new Object[]{ id, "", key } ) ) );
         // Test entry conversion
         testConversion();
      } catch ( Exception ex ) {
         log.log( Level.WARNING, "Error when testing conversion of {0}: {1}", new Object[]{ category.id, Utils.stacktrace( ex ) });
      }
   }

   protected final Matcher regxNote = Pattern.compile( "\\(.+?\\)|\\[.+?\\]|,.*| -.*", Pattern.CASE_INSENSITIVE ).matcher( "" );

   protected Set<String> getLookupName ( Entry entry, Set<String> list ) {
      list.add( regxNote.reset( entry.getName() ).replaceAll( "" ).trim() );
      return list;
   }

   public static Converter getConverter ( Category category ) {
      switch ( category.id ) {
         case "Background":
            return new BackgroundConverter( category );
         case "Class":
            return new ClassConverter( category );
         case "Companion":
            return new CompanionConverter( category ); // Sort by first field
         case "Deity":
            return new DeityConverter( category );
         case "EpicDestiny":
         case "ParagonPath":
            return new PPEDConverter( category );
         case "Feat":
            return new FeatConverter( category );
         case "Glossary":
            return new GlossaryConverter( category );
         case "Item":
         case "Armor":
         case "Implement":
         case "Weapon":
            return new ItemConverter( category );
         case "Monster":
            return new MonsterConverter( category );
         case "Ritual":
         case "Poison":
         case "Disease":
            return new LeveledConverter( category );
         case "Power":
            return new PowerConverter( category );
         case "Race":
            return new RaceConverter( category );
         case "Theme":
            return new ThemeConverter( category );
         case "Trap":
            return new TrapConverter( category );
         default:
            return new Converter( category );
      }
   }

   protected Convert ( Category category ) {
      this.category = category;
   }

   public void convert () throws InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Converting {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });
      initialise();
      final List<Entry> entries = category.entries;
      for ( Entry entry : entries ) {
         try { synchronized ( entry ) {
            this.entry = entry;
            convertEntry();
            if ( ! corrections.isEmpty() ) {
               if ( entry.getId().equals( "weapon147" ) ) // Duplicate of Arrow of Fate
                  corrections.clear();
               for ( String fix : corrections )
                  corrected( entry, fix );
               if ( corrections.size() > 1 )
                  corrected( entry, "multiple fix " + corrections.size() + " (bookkeep)" );
               corrections.clear();
            }
         } } catch ( Exception e ) {
            throw new UnsupportedOperationException( "Error converting " + entry, e );
         }
         if ( stop.get() ) throw new InterruptedException();
      }

      beforeSort();
      try {
         category.entries.sort( this::sortEntity );
      } catch ( Exception e ) {
         throw new UnsupportedOperationException( "Error sorting " + category, e );
      }
      if ( stop.get() ) throw new InterruptedException();
   }

   /**
    * Called at the beginning of entity conversion.  Will be called in every export.
    */
   protected void initialise()  {}

   /**
    * Called at the end of entity conversion but before sort.  Will be called in every export.
    */
   protected void beforeSort()  { }

   protected int sortEntity ( Entry a, Entry b ) {
      return a.getName().compareTo( b.getName() );
   }

   /**
    * Apply common conversions to entry data.
    */
   protected void convertEntry () {
      if ( entry.getName().contains( "’" ) )
         entry.setName( entry.getName().replace( "’", "'" ) );
      if ( entry.getId().contains( ".aspx" ) )
         entry.setId( entry.getId().replace( ".aspx?id=", "" ).toLowerCase() );
      if ( entry.getContent() != null )
         entry.setContent( normaliseData( entry.getContent() ) );
      correctEntry();
      parseSourceBook();
      // Converter will do some checking if debug is on.
   }

   /**
    * A chance to double check result of converts, fixes, sorts, etc.
    * Log a warning if any exception is thrown here, would not stop the export process.
    */
   protected void testConversion() {
   }

   /**
    * Log correction and keep track of correction count.
    * @param entry Fixed entry.
    * @param fix Type of fix.
    */
   private static void corrected ( Entry entry, String fix ) {
      log.log( Level.FINE, "Corrected {0} ({1})", new Object[]{ entry, fix });
      synchronized ( fixCount ) {
         if ( fixCount.containsKey( fix ) ) fixCount.get( fix ).incrementAndGet();
         else fixCount.put( fix, new AtomicInteger( 1 ) );
         fixedEntry.add( entry.getId() );
      }
   }

   /**
    * Entry specific data fixes. No need to call super when overriden.
    */
   protected abstract void correctEntry ();

   /**
    * Remove / convert images, unicode, and redundent whitespace
    * @param data Text data to normalise
    * @return Normalided data
    */
   protected abstract String normaliseData ( String data );

   /**
    * Read the sourcebook meta data and convert to abbreviated form.
    */
   protected abstract void parseSourceBook ();

   /**
    * Convert HTML data into full text data for full text search.
    * To conserve memory, this is called by exporter on demand, instead of mass convert before export.
    *
    * @param data Data to strip
    * @return Text data
    */
   public abstract String textData ( String data );
}