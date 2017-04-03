package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;

public class CompanionConverter extends Converter {

   public CompanionConverter ( Category category ) {
      super( category );
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( 0 ).compareTo( b.getSimpleField( 0 ) );
      return diff == 0 ? super.sortEntity( a, b ) : diff;
   }
}
