package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sort by a field, then by name.
 */
public class FieldSortConverter extends Converter {

   private final int SORT_FIELD;

   public FieldSortConverter(Category category, int sort_field, boolean debug) {
      super(category, debug);
      SORT_FIELD = sort_field;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ SORT_FIELD ].toString().compareTo( b.meta[ SORT_FIELD ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry () {
      switch ( category.id ) {
      case "Glossary":
         if ( entry.shortid.startsWith( "skill" ) ) { // Fix skills missing "improvising with" title
            if ( ! find( "IMPROVISING WITH" ) ) {
               entry.data = Pattern.compile( "<p class=flavor>(?!.*<p class=flavor>)" ).matcher( entry.data ).replaceFirst( "<h3>IMPROVISING WITH "+entry.name.toUpperCase()+"</h3><p class=flavor>" );
               fix( "missing content" );
            }
         }
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      if ( category.id.equals( "Glossary" ) ) {
         String name = entry.name;
         switch ( entry.shortid ) {
            case "glossary86": // Teleportation
               return new String[]{ "Teleport", "Teleportation" };
            case "glossary159": // Hit Points
               return new String[]{ "HP", "Hit Point", "Bloodied" };
            case "glossary179": // Defense Scores
               return new String[]{ "Defense Scores", "Defenses", "Defense", "Fortitude", "Reflex", "Will" };
            case "glossary341": // Dying and Death
               return new String[]{ "Dying", "Death", "Death Saving Throw", "Die", "Dies", "Kill", "Drop to 0 or" };
            case "glossary487": // Carrying, Lifting and Dragging
               return new String[]{ "Carry", "Carrying", "Lift", "Lifting", "Drag", "Dragging", "Normal Load", "Heavy Load", "Maximum Drag Load" };
            case "glossary622": // Action Types
               return new String[]{ "Standard Action", "Move Action", "Minor Action", "Immediate Reaction", "Immediate Action", "Immediate Interrupt", "Opportunity Action", "Free Action" };
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
      return super.getLookupName( entry );
   }

}