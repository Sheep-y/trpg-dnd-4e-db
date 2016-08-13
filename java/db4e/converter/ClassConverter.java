package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassConverter extends Converter {

   public ClassConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   private final Matcher regxClassFeatures = Pattern.compile( "<b>(?:Class Features?|Hybrid Talent Options?):? ?</b>:?([^<.]+)", Pattern.CASE_INSENSITIVE ).matcher( "" );
   static Map<String, Set<String>> featureMap = new HashMap<>( 77, 1f );

   @Override protected void convertEntry( Entry entry ) {
      super.convertEntry( entry );
      regxClassFeatures.reset( entry.data );
      Set<String> features = featureMap.get( entry.shortid );
      if ( features == null ) featureMap.put( entry.shortid, features = new HashSet<>() );
      while ( regxClassFeatures.find() ) {
         String[] names = regxClassFeatures.group( 1 ).trim().split( ",| or " );
         for ( String name : names ) {
            name = name.replaceFirst( "\\(.*\\)", "" ).trim();
            if ( name.endsWith( " Armor Proficiency" ) ) continue;
            features.add( name );
         }
      }
      if ( features.isEmpty() )
         log.log( Level.WARNING, "Class features not found: {0} {1}", new Object[]{ entry.shortid, entry.name });
   }
}