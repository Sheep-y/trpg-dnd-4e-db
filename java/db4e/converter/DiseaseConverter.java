package db4e.converter;

import db4e.data.Category;

public class DiseaseConverter extends LeveledConverter {

   public DiseaseConverter ( Category category ) {
      super( category );
   }

   @Override protected void correctEntry () {
      if ( find( "</i><br><p>") )
         swap( "</i><br></p>", "</i></p>" ); // Flavor text tag removal; does not affect layout so not count as a fix
      super.correctEntry();
   }
}