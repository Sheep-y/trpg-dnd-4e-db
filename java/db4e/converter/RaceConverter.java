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
      category.meta = new String[]{ "DescriptionAttribute", "Size", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxAbility  = Pattern.compile( "<b>Ability scores</b>: ([^<]+)" ).matcher( "" );

   @Override protected void convertEntry( Entry entry ) {
      entry.meta = new Object[]{ "", entry.fields[1], entry.fields[2] };
      super.convertEntry(entry);
      if ( entry.fields[1].isEmpty() ) {
         entry.meta[1] = "Medium";
         corrections.add( "meta" );
      }
      if ( regxAbility.reset( entry.data ).find() ) {
         String ability = regxAbility.group(1).replace( "+2 ", "" );
         if ( ability.endsWith( "choice" ) ) {
            ability = "Any";
         } else {
            ability = ability.replace( "Strength", "Str" );
            ability = ability.replace( "Constitution", "Con" );
            ability = ability.replace( "Dexterity", "Dex" );
            ability = ability.replace( "Intelligence", "Int" );
            ability = ability.replace( "Wisdom", "Wis" );
            ability = ability.replace( "Charisma", "Cha" );
         }
         entry.meta[0] = ability;
      } else {
         if ( entry.name.endsWith( "Draconian" ) )
            entry.meta[0] = "Cha, Con or Str";
         else if ( entry.name.endsWith( "Dwarf" ) )
            entry.meta[0] = "Con, Str or Wis";
         else if ( entry.name.endsWith( "Elf" ) )
            entry.meta[0] = "Dex, Int or Wis";
         else // Eladrin
            entry.meta[0] = "Int, Cha or Dex";
         corrections.add( "meta" );
      }
   }
}