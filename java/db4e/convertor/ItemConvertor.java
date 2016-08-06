package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class ItemConvertor extends LeveledConvertor {

   private static final int CATEGORY = 0;
   private final boolean isGeneric;

   public ItemConvertor ( Category category, boolean debug ) {
      super( category, debug ); // Sort by category
      isGeneric = category.id.equals( "Item" );
   }

   @Override public void initialise () {
      if ( isGeneric )
         category.meta = new String[]{ "Category", "Type" ,"Level", "Cost", "Rarity", "SourceBook" };
      super.initialise();
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      if ( isGeneric ) {
         int diff = a.meta[ CATEGORY ].toString().compareTo( b.meta[ 0 ].toString() );
         if ( diff != 0 ) return diff;
      }
      return super.sortEntity( a, b );
   }

   private final Matcher regxPowerFrequency = Pattern.compile( "✦\\s*\\(" ).matcher( "" );
   private final Matcher regxWhichIsReproduced = Pattern.compile( " \\([^)]+\\), which is reproduced below(?=.)" ).matcher( "" );

   @Override protected void convertEntry ( Entry entry ) {
      if ( isGeneric ) {
         String[] fields = entry.fields;
         entry.meta = new Object[]{ fields[0], "", fields[1], fields[2], fields[3], fields[4] };
      }
      super.convertEntry( entry );
      if ( ! isGeneric )
         entry.shortid = entry.shortid.replace( "item", category.id.toLowerCase() );
      // Group Items
      switch ( entry.meta[0].toString() ) {
         case "Arms" :
            if ( category.id.equals( "Armor" ) )
               entry.meta[0] = "Shield";
            else
               entry.meta[1] = "Bracers";
            break;

         case "Weapon" :
            if ( category.id.equals( "Implement" ) ) { // Superior implement
               entry.meta[0] = Utils.ucfirst( entry.name.replaceFirst( "^\\w+ ", "" ) );
               if ( entry.meta[0].equals( "Symbol" ) ) entry.meta[0] = "Holy Symbol";
               corrections.add( "recategorise" );
            }
            break;

         case "Wondrous" :
            if ( entry.name.contains( "Tattoo" ) )
               entry.meta[1] = "Tattoo";
      }
   }

   @Override protected void correctEntry ( Entry entry ) {
      if ( ! regxPublished.reset( entry.data ).find() ) {
         entry.data += "<p class=publishedIn>Published in " + entry.meta[ 4 ]  + ".</p>";
         corrections.add( "missing published" );
      }

      if ( entry.data.contains( ", which is reproduced below." ) ) {
         entry.data = regxWhichIsReproduced.reset( entry.data ).replaceFirst( "" );
         corrections.add( "consistency" );
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