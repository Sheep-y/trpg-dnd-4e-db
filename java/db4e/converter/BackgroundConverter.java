package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class BackgroundConverter extends Converter {

   public BackgroundConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise() {
      category.meta = new String[]{ "Type", "Campaign", "Benefit", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxBenefit  = Pattern.compile( "<i>Benefit: </i>([^<(.]+)" ).matcher( "" );
   private final String[] trims = { "list of", "list", "checks?", "bonus", "additional",
      "if you[^,]+, ", " rather than your own", "of your choice", ", allowing[^:]+:",
      "you gain a", "you are", "(?<!kill )your?" };
   private final Matcher regxTrim  = Pattern.compile( "(?:\\s|\\b)(?:" + String.join( "|", trims ) + ")\\b", Pattern.CASE_INSENSITIVE ).matcher( "" );

   @Override protected void convertEntry () {
      super.convertEntry();
      if ( meta( 2 ).isEmpty() && regxBenefit.reset( entry.data ).find() ) {
         String associate = regxTrim.reset( regxBenefit.group( 1 ) ).replaceAll( "" ).trim();
         if ( ! associate.endsWith( "." ) ) associate += '.';
         meta( 2, Utils.ucfirst( associate.replace( "saving throws", "saves" ) ) );
      } else {
         meta( 2, "Associated: " + entry.meta[2].toString().replace( "you can", "" ) );
      }
      if ( meta( 1 ).equals( "Scales of War Adventure Path" ) )
         meta( 1, "Scales of War" );
      else if ( meta( 1 ).equals( "Forgotten Realms" ) )
         meta( 1, "Faerûn" );
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      return new String[]{ entry.name };
   }

}