package db4e.converter;

import db4e.data.Category;

public class RitualConverter extends LeveledConverter {

   public RitualConverter ( Category category ) {
      super( category );
   }

   @Override protected void correctEntry () {
      switch ( entry.getId() ) {
         case "ritual288": // Primal Grove
            swap( " grp to ", " gp to ", "typo" );
            break;
      }
      super.correctEntry();
   }
}
