package db4e.converter;

import db4e.data.Category;
import sheepy.util.Utils;

/**
 */
public class PPEDConverter extends Converter {

   private static final int PREREQUISITE = 0;

   public PPEDConverter( Category category ) {
      super( category );
   }

   @Override protected void correctEntry() {
      if ( category.id.equals( "EpicDestiny" ) ) {
         String prereq = meta( PREREQUISITE );
         if ( prereq.startsWith( "21st level" ) || prereq.startsWith( "21st-level" ) ) {
            prereq = prereq.substring( 10 );
            if ( prereq.startsWith( ", " ) || prereq.startsWith( "; " ) )
               prereq = prereq.substring( 2 );
            prereq = prereq.trim();
            if ( prereq.length() > 0 )
               prereq = Utils.ucfirst( prereq );

            meta( PREREQUISITE, prereq );
            fix( "consistency" );
         }
      }

      switch ( entry.getId() ) {
         case "paragonpath153" : // Luckbringer of Tymora
            swap( "weakening<br>their", "weakening their" );
            fix( "formatting" );
            break;

         case "epicdestiny698" : // Unyielding Sentinel
            swap( "Defender role", "Any defender class" );
            meta( PREREQUISITE, "Any defender class" );
            fix( "consistency" );
            break;
      }

      super.correctEntry();
   }
}