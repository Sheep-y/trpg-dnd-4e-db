package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;

public class CompanionConverter extends CreatureConverter {

   public CompanionConverter ( Category category ) {
      super( category );
      SIZE = 1;
      TYPE = 2;
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "Type", "Size", "CreatureType", "SourceBook" };
      super.initialise();
   }

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( fields[0], "", "", fields[1] );
      super.convertEntry();
   }

   @Override protected void correctEntry () {
      if ( ! findSizeAndTypes() ) {
         if ( meta( 0 ).equals( "Companion" ) ) {
            meta( SIZE, "Medium" );
            meta( TYPE, "Beast, Natural" );
            switch ( entry.getId() ) {
               case "companion4": // Lizard
               case "companion6": // Serpent
                  meta( TYPE, "Beast, Natural, Reptile" );
                  break;
               case "companion5": // Raptor
                  meta( SIZE, "Small" );
                  break;
               case "companion54": // Vadalis-bred Griffon`
                  meta( SIZE, "Large" );
                  meta( TYPE, "Beast, Mount, Natural" );
                  break;
               case "companion90": // Horse
                  meta( SIZE, "Medium or Large" );
                  meta( TYPE, "Beast, Mount, Natural" );
                  break;
               case "companion91": // Simian
                  meta( SIZE, "Small or Medium" );
                  break;
            }
         } else { // Familiars
            meta( SIZE, "Tiny" );
         }
      }
      super.correctEntry();
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( 0 ).compareTo( b.getSimpleField( 0 ) );
      return diff == 0 ? super.sortEntity( a, b ) : diff;
   }
}
