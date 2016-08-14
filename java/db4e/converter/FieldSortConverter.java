package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.List;

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

   @Override protected void correctEntry(Entry entry) {
      switch ( category.id ) {
      case "Glossary":
         if ( entry.shortid.startsWith( "skill" ) ) { // Fix skills missing "improvising with" title
            if ( entry.data.contains( "<p class=flavor><b></b></p><p class=flavor>" ) ) {
               entry.data = entry.data.replace( "<p class=flavor><b></b></p><p class=flavor>", "<h3>IMPROVISING WITH "+entry.name.toUpperCase()+"</h3><p class=flavor>" );
               corrections.add( "missing content" );
            }
         }
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      if ( category.id.equals( "Glossary" ) ) {
         String name = entry.name;
         if ( entry.shortid.equals( "glossary159" ) ) // Teleportation
            return new String[]{ "Teleport", "Teleportation" };
         else if ( entry.shortid.equals( "glossary159" ) ) // Hit Points
            return new String[]{ "HP", "Hit Point", "Bloodied" };
         else if ( entry.shortid.equals( "glossary487" ) ) // Carrying, Lifting and Dragging
            return new String[]{ "Carry", "Carrying", "Lift", "Lifting", "Drag", "Dragging", "Normal Load", "Heavy Load", "Maximum Drag Load" };
         else if ( entry.shortid.equals( "glossary622" ) ) // Action Types
            return new String[]{ "Standard Action", "Move Action", "Minor Action", "Immediate Reaction", "Immediate Action", "Immediate Interrupt", "Opportunity Action", "Free Action" };
         else if ( name.endsWith( " speed" ) || name.endsWith( " Attack" ) )
            return new String[]{ name.substring( 0, name.length() - 6 ) };
         List<String> result = new ArrayList<>( 3 );
         result.add( name );
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