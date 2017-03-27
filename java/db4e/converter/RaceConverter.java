package db4e.converter;

import db4e.data.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaceConverter extends Converter {

   private static final int ORIGIN = 0;
   private static final int ABILITY = 1;
   private static final int SIZE = 2;

   public RaceConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise() {
      category.meta = new String[]{ "Origin", "DescriptionAttribute", "Size", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxAbility  = Pattern.compile( "<b>Ability scores</b>: ([^<]+)" ).matcher( "" );

   @Override protected void convertEntry () {
      meta( null, null, entry.getField( 1 ), entry.getField( 2 ) );
      super.convertEntry();
      // Origin column
      switch ( entry.getId() ) {
         case "race3":  // Eladrin
         case "race4":  // Elf
         case "race16": // Drow
         case "race20": // Gnome
         case "race44": // Wilden
         case "race56": // Moon Elf
         case "race57": // Sun Elf
         case "race58": // Wild Elf
         case "race59": // Wood Elf
         case "race60": // Hamadryad
         case "race61": // Pixie
         case "race62": // Satyr
         case "race64": // Llewyrr Elf
         case "race66": // Svirfneblin
            meta( ORIGIN, "Fey" );
            break;
         case "race14": // Changeling
            meta( ORIGIN, "Natural, shapechanger" );
            break;
         case "race23": // Kobold
            meta( ORIGIN, "Natural, reptile" );
            break;
         case "race26": // Shadar-kai
         case "race52": // Shade
            meta( ORIGIN, "Shadow" );
            break;
         case "race33": // Genasi
            meta( ORIGIN, "Elemental" );
            break;
         case "race35": // Deva
            meta( ORIGIN, "Immortal" );
            break;
         case "race47": // Revenant
         case "race53": // Vryloka
            meta( ORIGIN, "Undead, living" );
            break;
         case "race49": // Shardmind
            meta( ORIGIN, "Immortal, construct" );
            break;
         case "race65": // Hengeyokai
            meta( ORIGIN, "Fey, shapechanger" );
            break;
         default:
            meta( ORIGIN, "Natural" );
      }
      // Ability column
      if ( find( regxAbility ) ) {
         String ability = regxAbility.group(1).replace( "+2 ", "" );
         if ( ability.endsWith( "choice" ) )
            meta( ABILITY, "Any" );
         else
            meta( ABILITY, shortenAbility( ability ) );

      } else {
         if ( entry.getName().endsWith( " Draconian" ) )
            meta( ABILITY, "Cha, Con or Str" );
         else if ( entry.getName().endsWith( " Dwarf" ) )
            meta( ABILITY, "Con, Str or Wis" );
         else if ( entry.getName().endsWith( " Elf" ) )
            meta( ABILITY, "Dex, Int or Wis" );
         else // Eladrin
            meta( ABILITY, "Int, Cha or Dex" );
         fix( "missing meta" );
      }
      // Size column
      if ( entry.getField( 1 ).isEmpty() ) {
         meta( SIZE, "Medium" );
         fix( "missing meta" );
      }
   }
}