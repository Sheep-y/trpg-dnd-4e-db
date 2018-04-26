package db4e.converter;

import db4e.data.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RitualConverter extends LeveledConverter {
   private static final int TYPE = 0;

   public RitualConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "RitualType", "Level", "ComponentCost", "Price", "KeySkillDescription", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxCategory = Pattern.compile( "<b>Category</b>: ([^<]+)" ).matcher( "" );
   private final Matcher regxRitualStats = Pattern.compile( "<p><span class=ritualstats>(.+?)</span>(.+?)</p>" ).matcher( "" );

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( "", fields[0], fields[1], fields[2], fields[3], "" );
      super.convertEntry();
   }

   @Override protected void correctEntry () {
      switch ( entry.getId() ) {
         case "ritual288": // Primal Grove
            swap( " grp to ", " gp to ", "typo" );
            break;
      }

      locate( regxCategory );
      String type = regxCategory.group( 1 ).trim(), cat = "Ritual";
      switch ( type ) {
         case "Martial Practice":
            cat = "Martial Practice";
            // fallthrough
         case "Other":
            type = "";
            break;
         default:
            type = ", " + type;
      }
      if ( find( "Alchemical Item" ) )
         cat = "Alchemic";
      meta( TYPE, cat + type );

      // Swap left and right stats; level and category should go first!
      locate( regxRitualStats );
      data( regxRitualStats.replaceFirst( "<p><span class=ritualstats>$2</span>$1</p>" ) );

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
