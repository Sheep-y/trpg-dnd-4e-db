package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;

/**
 * Sort by a field, then by name.
 */
public class FieldSortConvertor extends Convertor {

   private final int SORT_FIELD;

   public FieldSortConvertor(Category category, int sort_field, boolean debug) {
      super(category, debug);
      SORT_FIELD = sort_field;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ SORT_FIELD ].toString().compareTo( b.meta[ SORT_FIELD ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

}