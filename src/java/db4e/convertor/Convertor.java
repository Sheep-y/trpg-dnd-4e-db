package db4e.convertor;

import db4e.Main;
import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert category and entry data for export
 */
public class Convertor {

   private static final Logger log = Main.log;

   protected Convertor () {} // Use getConvertor, don't new

   public static Convertor getConvertor ( Category category ) {
      return new Convertor();
   }

   public void convert ( Category category, ProgressState state ) {
      if ( category.meta == null )
         category.meta = category.fields;
      if ( category.sorted == null )
         category.sorted = new TreeSet<>( this::sortEntity );

      for ( Entry entry : category.entries ) {
         if ( entry.content == null ) throw new IllegalStateException( entry.name + " (" + category.name + ") has no content" );
         convertEntry( entry );
         state.addOne();
      }
   }

   protected int sortEntity ( Entry a, Entry b ) {
      return a.name.compareTo( b.name );
   }

   protected void convertEntry ( Entry entry ) {
      if ( entry.meta == null )
         entry.meta = entry.fields;
      if ( entry.data == null ) {
         entry.data = normaliseData( entry.content );
         if ( entry.data.contains( "<img " ) || entry.data.contains( "<a " ) )
            log.log( Level.WARNING, "Found image or link in {0} ({1})", new Object[]{ entry.id, entry.name } );
      }
      if ( entry.fulltext == null )
         entry.fulltext = textData( entry.data );
   }

   // Products, Magazines of "published in". May be site root (Class Compendium) or empty (associate.93/Earth-Friend)
   private Matcher regxSourceLink = Pattern.compile( "<a href=\"(?:http://www\\.wizards\\.com/[^\"]+)?\" target=\"_new\">([^<]+)</a>" ).matcher( "" );
   // Internal entry link, e.g. http://www.wizards.com/dndinsider/compendium/power.aspx?id=2848
   private Matcher regxEntryLink = Pattern.compile( "<a href=\"http://www.wizards.com/dndinsider/compendium/[^\"]+\">([^<]+)</a>" ).matcher( "" );
   // Internal search link, e.g. http://ww2.wizards.com/dnd/insider/item.aspx?fid=21&amp;ftype=3 - may also be empty (monster.2508/Darkpact Stalker)
   private Matcher regxSearchLink = Pattern.compile( "<a target=\"_new\" href=\"http://ww2.wizards.com/dnd/insider/[^\"]+\">([^<]*)</a>" ).matcher( "" );

   protected String normaliseData ( String data ) {
      // Convert nbsp to character
      data = data.replace( "&nbsp;", "\u00A0" );
      // Convert ’ to ' so that people can actually search for it
      data = data.replace( "’", "'" );
      // Replace images with character. Every image really appears in the compendium.
      data = data.replace( "<img src=\"images/bullet.gif\" alt=\"\">", "✦" ); // Four pointed star, 11x11
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/x.gif\">", "✦" ); // Four pointed star, 7x10
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/aura.png\" align=\"top\">", "☼" ); // Aura, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/aura.png\">", "☼" ); // Aura, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/S2.gif\">", "(⚔)" ); // Basic melee, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/S3.gif\">", "(➶)" ); // Basic ranged, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z1.gif\">" , "ᗕ" ); // Blast, 20x20, for 10 monsters
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z1a.gif\">", "ᗕ" ); // Blast, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z2a.gif\">", "⚔" ); // Melee, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z3a.gif\">", "➶" ); // Ranged, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z4.gif\">",  "✻" ); // Area, 20x20
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/Z4a.gif\">", "✻" ); // Area, 14x14
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/1a.gif\">", "⚀" ); // Dice 1, 12x12, honors go to monster.4611/"Rort, Goblin Tomeripper"
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/2a.gif\">", "⚁" ); // Dice 2, 12x12, 4 monsters got this
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/3a.gif\">", "⚂" ); // Dice 3, 12x12
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/4a.gif\">", "⚃" ); // Dice 4, 12x12
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/5a.gif\">", "⚄" ); // Dice 5, 12x12
      data = data.replace( "<img src=\"http://www.wizards.com/dnd/images/symbol/6a.gif\">", "⚅" ); // Dice 6, 12x12

      // Remove links
      data = regxSourceLink.reset( data ).replaceAll( "$1" );
      data = regxEntryLink .reset( data ).replaceAll( "$1" );
      data = regxSearchLink.reset( data ).replaceAll( "$1" );

      return data;
   }

   private final Matcher regxHtmlTag = Pattern.compile( "</?\\w+[^>]*>" ).matcher( "" );

   /**
    * Convert HTML data into full text data for full text search.
    *
    * @param data Data to strip
    * @return Text data
    */
   protected String textData ( String data ) {
      String result = regxHtmlTag.reset( data ).replaceAll( " " );
      if ( result.indexOf( '<' ) >= 0 || result.indexOf( '>' ) >= 0 ) {
         System.out.println( data );
         System.out.println( "----" );
         System.out.println( result );
         throw new AssertionError( "unclean tag" );
      }
      return result;
   }
}