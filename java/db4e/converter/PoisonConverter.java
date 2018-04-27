package db4e.converter;

import db4e.data.Category;

public class PoisonConverter extends LeveledConverter  {

   private static final int COST = 1;

   public PoisonConverter ( Category category ) {
      super( category );
   }

   @Override protected void correctEntry () {
      if ( find( "<p>Published in" ) ) {
         swap( "<p>Published in", "<p class=publishedIn>Published in", "styling" );
      }

      // Convert from item to poison
      if ( entry.getFieldCount() == 5 ) {
         meta( meta( 1 ), "", "" );
         entry.setId( entry.getId().replace( "item", "poison0" ) );
         swap( "<h1 class=mihead>", "<h1 class=poison>", "recategorise" );
      }

      if ( ! meta( COST ).isEmpty() ) {
         meta( COST, meta( COST ).replace( " GP", " gp" ) );
         fix( "consistency" );
      }

      switch ( entry.getId() ) {
         case "poison19": // Granny's Grief
            swap( ">Published in .<", ">Published in Dungeon Magazine 211.<", "missing published" );
            break;
         case "poison03562": // Gibbering Grind
         case "poison03564": // Umber Dust
         case "poison03565": // Heart of Mimic Powder
            swap( " (Consumable)", "" );
            // fallthrough
         case "poison03561": // Aboleth Slime Concentrate
         case "poison03563": // Grell Bile
         case "poison03566": // Mind Flayer Tentacle Extract
            swapAll( "(Consumable, ", "(" );
            swapAll( " ✦ (", " ✦ Consumable (", "missing power frequency" );
      }
      super.correctEntry();
   }
}
