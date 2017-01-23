package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossaryConverter extends FieldSortConverter {

   public GlossaryConverter ( Category category ) {
      super( category, 0 );
   }

   private final Matcher regxFlavor = Pattern.compile( "<p class=flavor>(?!.*<p class=flavor>)" ).matcher( "" );

   @Override protected void correctEntry () {
      if ( entry.shortid.startsWith( "skill" ) ) { // Fix skills missing "improvising with" title
         if ( ! find( "IMPROVISING WITH" ) ) {
            entry.data = regxFlavor.reset( entry.data ).replaceFirst( "<h3>IMPROVISING WITH "+entry.name.toUpperCase()+"</h3><p class=flavor>" );
            fix( "missing content" );
         }
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      String name = entry.name;
      switch ( entry.shortid ) {
         case "skill27": // Athletics
            return new String[]{ "Athletics", "Escape", "Climb", "Climbing", "Swim", "Swimming", "Jump", "Jumping", "Long Jump", "Long Jump" };
         case "glossary86": // Teleportation
            return new String[]{ "Teleport", "Teleportation" };
         case "glossary159": // Hit Points
            return new String[]{ "HP", "Hit Point", "Bloodied" };
         case "glossary163": // Sustained Durations
            return new String[]{ "Sustained Durations", "Sustain", "Sustain Minor", "Sustain Move", "Sustain Standard", "Sustain No Action" };
         case "glossary179": // Defense Scores
            return new String[]{ "Defense Scores", "Defenses", "Defense", "Fortitude", "Reflex", "Will" };
         case "glossary341": // Dying and Death
            return new String[]{ "Dying", "Death", "Death Saving Throw", "Die", "Dies", "Kill", "Drop to 0 or" };
         case "glossary487": // Carrying, Lifting and Dragging
            return new String[]{ "Carry", "Carrying", "Lift", "Lifting", "Drag", "Dragging", "Normal Load", "Heavy Load", "Maximum Drag Load" };
         case "glossary622": // Action Types
            return new String[]{ "Standard Action", "Move Action", "Minor Action", "Immediate Reaction", "Immediate Action", "Immediate Interrupt", "Opportunity Action", "Free Action" };
         case "glossary623": // Languages
            return new String[]{ "Language", "Script" };
         case "glossary670": // Magic Item Level and Rarity
            return new String[]{ "Magic Item Level and Rarity", "Common", "Uncommon", "Rare", "" };
      }
      if ( name.endsWith( " speed" ) || name.endsWith( " Attack" ) )
         return new String[]{ name.substring( 0, name.length() - 6 ) };
      List<String> result = new ArrayList<>( 3 );
      result.add( name );
      if ( name.startsWith( "Object" ) )
         result.add( "Object" );
      else if ( name.toLowerCase().contains( " size" ) )
         result.add( "size" );
      if ( name.endsWith( "s" ) ) {
         result.add( name.substring( 0, name.length() - 1 ) );
         if ( name.endsWith( "es" ) ) {
            result.add( name.substring( 0, name.length() - 2 ) );
            if ( name.endsWith( "ies" ) )
               result.add( name.substring( 0, name.length() - 3 ) + 'y' );
         }
      } else if ( name.endsWith( "ing" ) )
         result.add( name.substring( 0, name.length() - 3 ) );
      return result.toArray( new String[ result.size() ] );
   }
}
