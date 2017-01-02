package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaceConverter extends Converter {

   public RaceConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise() {
      category.meta = new String[]{ "Origin", "DescriptionAttribute", "Size", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxAbility  = Pattern.compile( "<b>Ability scores</b>: ([^<]+)" ).matcher( "" );

   @Override protected void convertEntry( Entry entry ) {
      entry.meta = new Object[]{ null, null, entry.fields[1], entry.fields[2] };
      super.convertEntry(entry);
      // Origin column
      switch ( entry.shortid ) {
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
            entry.meta[0] = "Fey";
            break;
         case "race14": // Changeling
            entry.meta[0] = "Natural, shapechanger";
            break;
         case "race23": // Kobold
            entry.meta[0] = "Natural, reptile";
            break;
         case "race26": // Shadar-kai
         case "race52": // Shade
            entry.meta[0] = "Shadow";
            break;
         case "race33": // Genasi
            entry.meta[0] = "Elemental";
            break;
         case "race35": // Deva
            entry.meta[0] = "Immortal";
            break;
         case "race47": // Revenant
         case "race53": // Vryloka
            entry.meta[0] = "Undead, living";
            break;
         case "race49": // Shardmind
            entry.meta[0] = "Immortal, construct";
            break;
         case "race65": // Hengeyokai
            entry.meta[0] = "Fey, shapechanger";
            break;
         default:
            entry.meta[0] = "Natural";
      }
      // Ability column
      if ( regxAbility.reset( entry.data ).find() ) {
         String ability = regxAbility.group(1).replace( "+2 ", "" );
         if ( ability.endsWith( "choice" ) )
            entry.meta[1] = "Any";
         else
            entry.meta[1] = shortenAbility( ability );

      } else {
         if ( entry.name.endsWith( "Draconian" ) )
            entry.meta[1] = "Cha, Con or Str";
         else if ( entry.name.endsWith( "Dwarf" ) )
            entry.meta[1] = "Con, Str or Wis";
         else if ( entry.name.endsWith( "Elf" ) )
            entry.meta[1] = "Dex, Int or Wis";
         else // Eladrin
            entry.meta[1] = "Int, Cha or Dex";
         fix( "meta" );
      }
      // Size column
      if ( entry.fields[2].isEmpty() ) {
         entry.meta[2] = "Medium";
         fix( "meta" );
      }
   }
}