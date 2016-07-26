package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemConvertor extends FieldSortConvertor {

   public ItemConvertor ( Category category, boolean debug ) {
      super( category, 0, debug ); // Sort by category
   }

   Matcher regxPowerFrequency = Pattern.compile( "✦\\s*\\(" ).matcher( "" );

   @Override protected String correctEntry ( Entry entry ) {
      switch ( entry.shortid ) {
         case "item467": // Alchemical Failsafe
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ At-Will</h2>" );
            return "missing power frequency";

         case "item1007": // Dantrag's Bracers, first (arm) power is daily, second (feet) power is encounter
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Daily</h2>" );
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Encounter</h2>" );
            return "missing power frequency";

         case "item1006": // Dancing Weapon
         case "item1261": // Feral Armor
         case "item2451": // Shadowfell Blade
            entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
            return "basic attack correction";

         case "item1864": // Mirror of Deception
            entry.data = entry.data.replace( " ✦ (Standard", " ✦ At-Will (Standard" );
            entry.data = entry.data.replace( "alter</p><p class=\"mistat indent\">sound", "alter sound" );
            return "formatting";

         case "item3328": // Scepter of the Chosen Tyrant
            entry.data = entry.data.replace( "basic ranged attack", "ranged basic attack" );
            return "basic attack correction";

         case "item3561": // Aboleth Slime Concentrate
         case "item3562": // Gibbering Grind
         case "item3563": // Grell Bile
         case "item3564": // Umber Dust
         case "item3565": // Heart of Mimic Powder
         case "item3566": // Mind Flayer Tentacle Extract
            entry.data = entry.data.replace( " (Consumable)", "" ).replace( "(Consumable, ", "(" );
            entry.data = entry.data.replace( " ✦ (", " ✦ Consumable (" );
            return "missing power frequency";

         default:
            // Add "At-Will" to the ~150 items missing a power frequency.
            // I checked each one and the exceptions are above.
            if ( regxPowerFrequency.reset( entry.data ).find() ) {
               entry.data = regxPowerFrequency.replaceAll( "✦ At-Will (" );
               return "missing power frequency";
            }

            if ( debug ) {
               //if ( entry.data.contains( "Power ✦ <" ) || regxCheckPowerFrequency.reset( entry.data ).find() )
               //   log.log( Level.WARNING, "Power without frequency: {0} {1}", new Object[]{ entry.shortid, entry.name } );
            }
      }
      return null;
   }
}