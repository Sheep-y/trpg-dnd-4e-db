package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;

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
               corrections.add( "missing subtitle" );
            }
         }
      }
   }
}