package db4e.converter;

import db4e.data.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RitualConverter extends LeveledConverter {

   public RitualConverter ( Category category ) {
      super( category );
   }

   private final Matcher regxRitualStats = Pattern.compile( "<p><span class=ritualstats>(.+?)</span>(.+?)</p>" ).matcher( "" );

   @Override protected void correctEntry () {
      switch ( entry.getId() ) {
         case "ritual288": // Primal Grove
            swap( " grp to ", " gp to ", "typo" );
            break;
      }

      // Swap left and right stats; level and category should go first!
      if ( find( regxRitualStats ) )
         swap( regxRitualStats.group(), "<p><span class=ritualstats>" + regxRitualStats.group( 2 ) + "</span>" + regxRitualStats.group( 1 ) + "</p>" );
      else
         warn( "Ritual stats not found" );

      /* // Rebuild stats as table; gave up because not better than plain list
      if ( find( regxRitualStats ) ) {
         // Rewrite ritual stat to table
         StringBuilder buf = new StringBuilder( regxRitualStats.group().length() + 64 );
         String[] left = regxRitualStats.group( 1 ).split( "<br>" ), right = regxRitualStats.group( 2 ).split( "<br>" );

         buf.append( "<table class=ritualstats>" );
         int i = 0;
         for ( int len = Math.min( left.length, right.length ) ; i < len ; i++ )
            buf.append( "<tr><td>" ).append( right[i] ).append( "<td>" ).append( left[i] );
         if ( right.length > left.length )
            left = right;
         if ( i < left.length )
            for ( ; i < left.length ; i++ )
               buf.append( "<tr><td>" ).append( left[i] );
         buf.append( "</table>" );

         swap( regxRitualStats.group(), buf.toString() );
         buf.setLength( 0 );
      */

      super.correctEntry();
   }
}
