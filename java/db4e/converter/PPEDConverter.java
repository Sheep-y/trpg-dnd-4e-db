package db4e.converter;

import db4e.data.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class PPEDConverter extends Converter {

   private static final int PREREQUISITE = 0;

   public PPEDConverter( Category category ) {
      super( category );
   }

   private final Matcher regxBenefit  = Pattern.compile( "</h1><p><i>.*?</i></p>" ).matcher( "" );

   @Override protected void correctEntry() {
      if ( category.id.equals( "EpicDestiny" ) ) {
         String prereq = meta( PREREQUISITE );
         if ( prereq.startsWith( "21st level" ) || prereq.startsWith( "21st-level" ) ) {
            prereq = prereq.substring( 10 );
            if ( prereq.startsWith( ", " ) || prereq.startsWith( "; " ) )
               prereq = prereq.substring( 2 );
            prereq = prereq.trim();
            if ( prereq.length() > 0 )
            prereq = Character.toUpperCase( prereq.charAt( 0 ) ) + prereq.substring( 1 );

         } else if ( prereq.contains( " role" ) ) {
            prereq = prereq.replace( " role", "" );
         }
         if ( ! prereq.equals( meta( PREREQUISITE ) ) ) {
            meta( PREREQUISITE, prereq );
            fix( "consistency" );
         }
      }
      super.correctEntry();
   }

   @Override public String textData( String data ) {
      data = regxBenefit.reset( data ).replaceAll( "</h1>" );
      return super.textData( data );
   }
}