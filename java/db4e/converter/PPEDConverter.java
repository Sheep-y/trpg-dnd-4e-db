package db4e.converter;

import db4e.data.Category;
import sheepy.util.Utils;

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
            swap( "weakening<br>their", "weakening their", "formatting" );
            break;

         case "paragonpath175": // Horizon Walker
            swap( "weaopn", "weapon", "typo" );
            break;

         case "paragonpath648": // Animus Predator, Animus Strike
            swap( "<b>Implement</b>, <b>Primal</b><br>", "<b>Implement</b>, <b>Primal</b>, <b>Spirit</b><br>", "missing keyword" );
            break;

         case "epicdestiny698" : // Unyielding Sentinel
            swap( "Defender role", "Any defender class", "consistency" );
            meta( PREREQUISITE, "Any defender class" );
            break;
      }

      super.correctEntry();

      switch ( entry.getId() ) {
         case "paragonpath285" : // Verdant Lord
         case "paragonpath350" : // Familiar Keeper
         case "paragonpath361" : // Long Night Scion
         case "paragonpath364" : // Ghostwalker
         case "paragonpath371" : // Lightwalker
         case "paragonpath385" : // Thuranni Shadow Killer
         case "paragonpath458" : // Watcher of Vengeance
         case "paragonpath482" : // Coiled Serpent
         case "paragonpath531" : // Talaric Strategist
         case "paragonpath598" : // Umbral Cabalist
         case "paragonpath607" : // Death Arrow
         case "epicdestiny510" : // World Tree Guardian
            fixPowerFrequency();
            break;
      }
   }
}