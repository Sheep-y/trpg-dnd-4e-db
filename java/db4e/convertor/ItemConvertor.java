package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemConvertor extends LeveledConvertor {

   private static final int CATEGORY = 0;

   public ItemConvertor ( Category category, boolean debug ) {
      super( category, debug ); // Sort by category
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ CATEGORY ].toString().compareTo( b.meta[ 0 ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

   private final Matcher regxPowerFrequency = Pattern.compile( "✦\\s*\\(" ).matcher( "" );
   private final Matcher regxWhichIsReproduced = Pattern.compile( " \\([^)]+\\), which is reproduced below(?=.)" ).matcher( "" );

   @Override protected void correctEntry ( Entry entry ) {
      if ( ! regxPublished.reset( entry.data ).find() ) {
         entry.data += "<p class=publishedIn>Published in " + entry.meta[ 4 ]  + ".</p>";
         corrections.add( "missing published" );
      }

      if ( entry.data.contains( ", which is reproduced below." ) ) {
         entry.data = regxWhichIsReproduced.reset( entry.data ).replaceFirst( "" );
         corrections.add( "consistency" );
      }

      if ( entry.meta[CATEGORY].equals( "Arms" ) && entry.data.contains( ">Arms Slot: <" ) && entry.data.contains( " shield" ) ) {
         entry.meta[ CATEGORY ] = "Shield";
      } else if ( entry.meta[CATEGORY].equals( "Wondrous" ) && entry.name.contains( "Tattoo" ) ) {
         entry.meta[ CATEGORY ] = "Tattoo";
      }

      switch ( entry.shortid ) {
         case "item467": // Alchemical Failsafe
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ At-Will</h2>" );
            corrections.add( "missing power frequency" );
            break;

         case "item1007": // Dantrag's Bracers, first (arm) power is daily, second (feet) power is encounter
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Daily</h2>" );
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Encounter</h2>" );
            corrections.add( "missing power frequency" );
            break;

         case "item1006": // Dancing Weapon
         case "item1261": // Feral Armor
         case "item2451": // Shadowfell Blade
            entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "item1864": // Mirror of Deception
            entry.data = entry.data.replace( " ✦ (Standard", " ✦ At-Will (Standard" );
            entry.data = entry.data.replace( "alter</p><p class=\"mistat indent\">sound", "alter sound" );
            corrections.add( "formatting" );
            break;

         case "item3328": // Scepter of the Chosen Tyrant
            entry.data = entry.data.replace( "basic ranged attack", "ranged basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "item3415": // The Fifth Sword of Tyr
            entry.data = entry.data.replace( "Power (Teleportation) ✦ Daily", "Power (Weapon) ✦ Daily" );
            corrections.add( "typo" );
            break;

         default:
            // Add "At-Will" to the ~150 items missing a power frequency.
            // I checked each one and the exceptions are above.
            if ( regxPowerFrequency.reset( entry.data ).find() ) {
               entry.data = regxPowerFrequency.replaceAll( "✦ At-Will (" );
               corrections.add( "missing power frequency" );
            }
      }
   }
}