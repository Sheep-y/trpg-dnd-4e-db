package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;

public class PowerConvertor extends LeveledConvertor {

   private int CLASS = -1;

   public PowerConvertor(Category category, boolean debug) {
      super(category, debug);
   }

   @Override public void initialise () {
      CLASS = metaIndex( "ClassName" );
      if ( CLASS < 0 ) throw new IllegalStateException( "ClassName field not in " + category.name );
      super.initialise();
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ CLASS ].toString().compareTo( b.meta[ CLASS ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

}