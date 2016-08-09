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

   private final Matcher regxBenefit  = Pattern.compile( "<i>Benefit: </i>([^<(]+)" ).matcher( "" );
   private final Matcher regxTrim  = Pattern.compile( "(?:\\s|\\b)(?:you gain a|your?|list of|list)\\b", Pattern.CASE_INSENSITIVE ).matcher( "" );

   @Override protected void convertEntry( Entry entry ) {
      super.convertEntry(entry);
      if ( entry.meta[2].toString().isEmpty() && regxBenefit.reset( entry.data ).find() ) {
         entry.meta[2] = Utils.ucfirst( regxTrim.reset( regxBenefit.group( 1 ) ).replaceAll( "" ).trim() );
      }
      if ( entry.meta[1].equals( "Scales of War Adventure Path" ) )
         entry.meta[1] = "Scales of War";
      else if ( entry.meta[1].equals( "Forgotten Realms" ) )
         entry.meta[1] = "Faerûn";
   }
}