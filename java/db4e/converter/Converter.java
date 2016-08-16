package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default entry handling goes here.
 */
public class Converter extends Convert {

   protected final boolean debug;

   public Converter ( Category category, boolean debug ) {
      super( category );
      this.debug = debug;
   }

   @Override protected void correctEntry ( Entry entry ) {
   }

   private final Matcher regxCheckFulltext = Pattern.compile( "<\\w|(?<=\\w)>|&[^D ]" ).matcher( "" );
   private final Matcher regxCheckOpenClose = Pattern.compile( "<(/?)(p|span|b|i)\\b" ).matcher( "" );
   private final Matcher regxCheckDate  = Pattern.compile( "\\(\\d+/\\d+/\\d+\\)" ).matcher( "" );
   private final Map<String, Entry> shortId = new HashMap<>();

   /**
    * Apply common conversions to entry data.
    * entry.meta may be set, but other prorerties will be overwritten.
    *
    * @param entry
    */
   @Override protected void convertEntry ( Entry entry ) {
      super.convertEntry( entry );
      if ( debug ) {
         // These checks are enabled only when debug log is showing, mainly for development and debug purpose.
         if ( shortId.containsKey( entry.shortid ) )
            log.log( Level.WARNING, "{1} duplicate shortid '{2}': {3} & {0}", new Object[]{ entry.id, entry.name, entry.shortid, shortId.get( entry.shortid ).name } );
         else
            shortId.put( entry.shortid, entry );

         // Validate content tags
         if ( entry.data.contains( "<img " ) || entry.data.contains( "<a " ) )
            log.log( Level.WARNING, "Unremoved image or link in {0} {1}", new Object[]{ entry.shortid, entry.name } );

         int unclosed_p = 0, unclosed_span = 0, unclosed_b = 0, unclosed_i = 0;
         regxCheckOpenClose.reset( entry.data );
         while ( regxCheckOpenClose.find() ) {
            switch( regxCheckOpenClose.group( 2 ) ) {
               case "p":    unclosed_p += regxCheckOpenClose.group( 1 ).isEmpty() ? 1 : -1 ; break;
               case "span": unclosed_span += regxCheckOpenClose.group( 1 ).isEmpty() ? 1 : -1 ; break;
               case "b":    unclosed_b += regxCheckOpenClose.group( 1 ).isEmpty() ? 1 : -1 ; break;
               case "i":    unclosed_i += regxCheckOpenClose.group( 1 ).isEmpty() ? 1 : -1 ; break;
            }
         }
         if ( ( unclosed_p | unclosed_span | unclosed_p | unclosed_i ) != 0 )
            log.log( Level.WARNING, "Unbalanced open and closing bracket in {0} ({1})", new Object[]{ entry.shortid, entry.name } );

         // Validate fulltext
         if ( regxCheckFulltext.reset( entry.fulltext ).find() )
            log.log( Level.WARNING, "Unremoved html tag in fulltext of {0} ({1})", new Object[]{ entry.shortid, entry.name } );
         if ( regxCheckDate.reset( entry.fulltext ).find() )
            log.log( Level.WARNING, "Unremoved errata date in fulltext of {0} ({1})", new Object[]{ entry.shortid, entry.name } );
         if ( ! entry.fulltext.endsWith( "." ) ) // Item144 & Item152 fails this check
            log.log( Level.WARNING, "Not ending in full stop: {0} ({1})", new Object[]{ entry.shortid, entry.name } );
      }
   }

   private static final Map<String, String> books = new HashMap<>();

   static {
      books.put( "Adventurer's Vault", "AV" );
      books.put( "Adventurer's Vault 2", "AV2" );
      books.put( "Arcane Power", "AP" );
      books.put( "Dark Sun Campaign Setting", "DSCS" );
      books.put( "Dark Sun Creature Catalog", "DSCC" );
      books.put( "Divine Power", "DP" );
      books.put( "Dragons of Eberron", "DoE" );
      books.put( "Draconomicon: Chromatic Dragons", "Draconomicon: Chromatic" );
      books.put( "Draconomicon: Metallic Dragons", "Draconomicon: Metallic" );
      books.put( "Dungeon Delve", "DD" );
      books.put( "Dungeon Master's Guide", "DMG" );
      books.put( "Dungeon Master's Guide 2", "DMG2" );
      books.put( "Dungeon Master's Kit", "DMK" );
      books.put( "E1 Death's Reach", "E1" );
      books.put( "E2 Kingdom of the Ghouls", "E2" );
      books.put( "E3 Prince of Undeath", "E3" );
      books.put( "Eberron Campaign Setting", "ECS" );
      books.put( "Eberron Player's Guide", "EPG" );
      books.put( "FR1 Scepter Tower of Spellgard", "FR1" );
      books.put( "Forgotten Realms Campaign Guide", "FRCG" );
      books.put( "Forgotten Realms Player's Guide", "FRPG" );
      books.put( "H1 Keep on the Shadowfell", "H1" );
      books.put( "H2 Thunderspire Labyrinth", "H2" );
      books.put( "H3 Pyramid of Shadows", "H3" );
      books.put( "HS1 The Slaying Stone", "HS1" );
      books.put( "HS2 Orcs of Stonefang Pass", "HS2" );
      books.put( "Heroes of Shadow", "HoS" );
      books.put( "Heroes of the Elemental Chaos", "HotEC" );
      books.put( "Heroes of the Fallen Lands", "HotFL" );
      books.put( "Heroes of the Feywild", "HotF" );
      books.put( "Heroes of the Forgotten Kingdoms", "HotFK" );
      books.put( "Into the Unknown: The Dungeon Survival Handbook", "DSH" );
      books.put( "Manual of the Planes", "MotP" );
      books.put( "Martial Power", "MP" );
      books.put( "Martial Power 2", "MP2" );
      books.put( "Monster Manual", "MM" );
      books.put( "Monster Manual 2", "MM2" );
      books.put( "Monster Manual 3", "MM3" );
      books.put( "Monster Vault", "MV" );
      books.put( "Monster Vault: Threats to the Nentir Vale", "MV:TttNV" );
      books.put( "Mordenkainen's Magnificent Emporium", "MME" );
      books.put( "Neverwinter Campaign Setting", "NCS" );
      books.put( "P1 King of the Trollhaunt Warrens", "P1" );
      books.put( "P2 Demon Queen Enclave", "P2" );
      books.put( "P3 Assault on Nightwyrm Fortress", "P3" );
      books.put( "Player's Handbook", "PHB" );
      books.put( "Player's Handbook 2", "PHB2" );
      books.put( "Player's Handbook 3", "PHB3" );
      books.put( "Player's Handbook Races: Dragonborn", "PHR:D" );
      books.put( "Player's Handbook Races: Tiefling", "PHR:T" );
      books.put( "Primal Power", "PP" );
      books.put( "Psionic Power", "PsP" );
      books.put( "PH Heroes: Series 1", "PHH:S1" );
      books.put( "PH Heroes: Series 2", "PHH:S2" );
      books.put( "Red Box Starter Set", "Red Box" );
      books.put( "Rules Compendium", "RC" );
      books.put( "The Plane Above", "TPA" );
      books.put( "The Plane Below", "TPB" );
      books.put( "The Shadowfell", "TS" );
      books.put( "Vor Rukoth: An Ancient Ruins Adventure Site", "Vor Rukoth" );
   }

   protected final Matcher regxPublished = Pattern.compile( "<p class=publishedIn>Published in ([^<>]+)</p>" ).matcher( "" );
   private final Matcher regxBook = Pattern.compile( "([A-Z][^,.]*)(?:, page[^,.]+|\\.)" ).matcher( "" );

   @Override protected void parseSourceBook ( Entry entry ) {
      if ( regxPublished.reset( entry.data ).find() ) {

         String published = regxPublished.group( 1 );
         StringBuilder sourceBook = new StringBuilder();
         String lastSource = "";
         regxBook.reset( published );
         while ( regxBook.find() ) {
            String book = regxBook.group( 1 ).trim();
            String abbr = books.get( book );
            if ( abbr == null ) {
               if ( book.equals( "Class Compendium" ) ) continue; // Never published
               if ( book.contains( " Magazine " ) )
                  abbr = book.replace( "gon Magazine ", "" ).replace( "geon Magazine ", "" );
               else synchronized ( book ) {
                  abbr = books.get( book );
                  if ( abbr == null ) {
                     books.put( book, book );
                     log.log( Level.FINE, "Source without abbrivation: {0} ({1})", new Object[]{ book, entry.shortid } );
                     abbr = book;
                  }
               }
            }
            if ( sourceBook.length() > 0 ) sourceBook.append( ", " );
            sourceBook.append( abbr );
            lastSource = abbr;
         }
         if ( lastSource.isEmpty() )
            if ( published.equals( "Class Compendium." ) )
               lastSource = "CC"; // 11 feats and 2 powers does not list any other source book, only class compendium.
            else
               log.log(Level.WARNING, "Entry with unparsed book: {0} {1} - {2}", new Object[]{ entry.shortid, entry.name, published} );
         entry.meta[ entry.meta.length-1 ] = sourceBook.indexOf( ", " ) > 0 ? sourceBook.toString() : lastSource;

      } else if ( entry.data.contains( "ublished in" ) ) {
         log.log( Level.WARNING, "Entry with unparsed source: {0} {1}", new Object[]{ entry.shortid, entry.name } );
      } else {
         log.log( Level.INFO, "Entry without source book: {0} {1}", new Object[]{ entry.shortid, entry.name } );
      }
   }

   // Products, Magazines of "published in". May be site root (Class Compendium) or empty (associate.93/Earth-Friend)
   //private final Matcher regxSourceLink = Pattern.compile( "<a href=\"(?:http://www\\.wizards\\.com/[^\"]+)?\" target=\"_new\">([^<]+)</a>" ).matcher( "" );
   // Internal entry link, e.g. http://www.wizards.com/dndinsider/compendium/power.aspx?id=2848
   //private final Matcher regxEntryLink = Pattern.compile( "<a href=\"http://www.wizards.com/dndinsider/compendium/[^\"]+\">([^<]+)</a>" ).matcher( "" );
   // Internal search link, e.g. http://ww2.wizards.com/dnd/insider/item.aspx?fid=21&amp;ftype=3 - may also be empty (monster.2508/Darkpact Stalker)
   //private final Matcher regxSearchLink = Pattern.compile( "<a target=\"_new\" href=\"http://ww2.wizards.com/dnd/insider/[^\"]+\">([^<]*)</a>" ).matcher( "" );
   // Combined link pattern
   private final Matcher regxLinks = Pattern.compile( "<a(?: target=\"_new\")? href=\"(?:http://ww[w2].wizards.com/[^\"]*)?\"(?: target=\"_new\")?>([^<]*)</a>" ).matcher( "" );

   private final Matcher regxAttr1 = Pattern.compile( "<(\\w+) (\\w+)=\"(\\w+)\">" ).matcher( "" );
   private final Matcher regxAttr2 = Pattern.compile( "<(\\w+) (\\w+)=\"(\\w+)\" (\\w+)=\"(\\w+)\">" ).matcher( "" );
   private final Matcher regxAttr3 = Pattern.compile( "<(\\w+) (\\w+)=\"([^'\"/]+)\">" ).matcher( "" );

   private final Matcher regxEmptyTag = Pattern.compile( "<(\\w+)[^>]*></\\1>" ).matcher( "" );

   @Override protected String normaliseData ( String data ) {
      // Replace images with character. Every image really appears in the compendium.
      data = data.replace( "<img src=\"images/bullet.gif\" alt=\"\">", "✦" ); // Four pointed star, 11x11, most common image at 100k hits
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/x.gif\">", "✦" ); // Four pointed star, 7x10, second most common image at 40k hits
      if ( data.contains( "<img " ) ) { // Most likely monsters
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/S2.gif\">", "(⚔) " ); // Basic melee, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/S3.gif\">", "(➶) " ); // Basic ranged, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z1.gif\">" , "ᗕ " ); // Blast, 20x20, for 10 monsters
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z1a.gif\">", "ᗕ " ); // Blast, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z2a.gif\">", "⚔ " ); // Melee, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z3a.gif\">", "➶ " ); // Ranged, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z4.gif\">",  "✻ " ); // Area, 20x20
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z4a.gif\">", "✻ " ); // Area, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/aura.png\" align=\"top\">", "☼ " ); // Aura, 14x14
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/aura.png\">", "☼ " ); // Aura, 14x14, ~1000?
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/1a.gif\">", "⚀" ); // Dice 1, 12x12, honors go to monster.4611/"Rort, Goblin Tomeripper"
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/2a.gif\">", "⚁" ); // Dice 2, 12x12, 4 monsters got this
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/3a.gif\">", "⚂" ); // Dice 3, 12x12, ~30
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/4a.gif\">", "⚃" ); // Dice 4, 12x12, ~560
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/5a.gif\">", "⚄" ); // Dice 5, 12x12, ~2100
         data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/6a.gif\">", "⚅" ); // Dice 6, 12x12, ~2500
      }
      // Convert spaces and breaks
      data = data.replace( "&nbsp;", "\u00A0" );
      data = data.replace( "<br/>", "<br>" ).replace( "<br />", "<br>" );
      data = regxSpaces.reset( data ).replaceAll( " " );
      // Convert ’ to ' so that people can actually search for it
      data = data.replace( "’", "'" );
      data = data.replace( "“’", "\"" );
      data = data.replace( "”’", "\"" );
      // Convert attribute="value" to attribute=value, for cleaner data
      data = regxAttr1.reset( data ).replaceAll( "<$1 $2=$3>" );
      data = regxAttr2.reset( data ).replaceAll( "<$1 $2=$3 $4=$5>" );
      // Convert attribute="value value" to attribute='value value', for cleaner data
      data = regxAttr3.reset( data ).replaceAll( "<$1 $2='$3'>" );
      // Remove empty tags (but not some empty cells which has a space)
      while ( regxEmptyTag.reset( data ).find() )
         data = regxEmptyTag.replaceAll( "" );
      // Convert some rare line breaks
      if ( data.indexOf( '\n' ) >= 0 ) {
         data = data.replace( "\n,", "," );
         data = data.replace( "\n.", "." );
         data = data.replace( ".\n", "." );
      }

      // Remove links
      //data = regxSourceLink.reset( data ).replaceAll( "$1" );
      //data = regxEntryLink .reset( data ).replaceAll( "$1" );
      //data = regxSearchLink.reset( data ).replaceAll( "$1" );
      data = regxLinks.reset( data ).replaceAll( "$1" );

      return data.trim();
   }

   private final Matcher regxPowerFlav = Pattern.compile( "(<h1 class=\\w{5,9}power>.*?</h1>)<p class=flavor><i>[^>]+</i></p>" ).matcher( "" );
   private final Matcher regxItemFlav  = Pattern.compile( "(<h1 class=mihead>.*?</h1>)<p class=miflavor>[^>]+</p>" ).matcher( "" );
   // Errata removal. monster217 has empty change, and many have empty action (Update/Added/Removed).
   private final Matcher regxErrata  = Pattern.compile( "<br>\\w* \\([123]?\\d/[123]?\\d/20[01]\\d\\)<br>[^<]*" ).matcher( "" );
   private final Matcher regxHtmlTag = Pattern.compile( "</?\\w+[^>]*>" ).matcher( "" );
   private final Matcher regxSpaces  = Pattern.compile( " +" ).matcher( " " );

   /**
    * Convert HTML data into full text data for full text search.
    *
    * @param data Data to strip
    * @return Text data
    */
   @Override protected String textData ( String data ) {
      // Removes excluded text
      if ( data.indexOf( "power>" ) > 0 ) // Power flavour
         data = regxPowerFlav.reset( data ).replaceAll( "$1" );
      if ( data.indexOf( "mihead>" ) > 0 ) // Magic item flavour
         data = regxItemFlav.reset( data ).replaceAll( "$1" );
      data = data.replace( "<p class=publishedIn>Published in", "" ); // Source book
      data = regxErrata.reset( data ).replaceAll( " " ); // Errata

      // Strip HTML tags then redundent spaces
      data = data.replace( '\u00A0', ' ' );
      data = regxHtmlTag.reset( data ).replaceAll( " " );
      data = regxSpaces.reset( data ).replaceAll( " " );

      // HTML unescape. Compendium has relatively few escapes.
      data = data.replace( "&amp;", "&" );
      data = data.replace( "&gt;", ">" ); // glossary.433/"Weapons and Size"

      return data.trim();
   }
}