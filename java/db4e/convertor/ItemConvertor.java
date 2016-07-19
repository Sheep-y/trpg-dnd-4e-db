package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;

public class ItemConvertor extends LeveledConvertor {

   private int GROUP = -1;

   public ItemConvertor(Category category, boolean debug) {
      super(category, debug);
   }

   @Override public void initialise () {
      GROUP = metaIndex( "Category" );
      if ( GROUP < 0 ) throw new IllegalStateException( "Category field not in " + category.name );
      super.initialise();
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ GROUP ].toString().compareTo( b.meta[ GROUP ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

}