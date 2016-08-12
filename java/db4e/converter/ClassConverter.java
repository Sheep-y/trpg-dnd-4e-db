package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import sheepy.util.Utils;

public class ClassConverter extends Converter {

   public ClassConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   private final Matcher regxFeatureName  = Pattern.compile( "<br><br><b>(Level \\d+ )?([^<(]+?)(?=\\s*[<(])" ).matcher( "" );
   static Map<String, Set<String>> featureMap = new HashMap<>( 77, 1f );

   @Override protected void convertEntry( Entry entry ) {
      super.convertEntry( entry );
      int pos = entry.data.indexOf( " FEATURE" );
      if ( pos > 0 ) {
         Set<String> features = featureMap.get( entry.shortid );
         String aData = entry.data;
         if ( features == null ) featureMap.put( entry.shortid, features = new HashSet<>() );
         regxFeatureName.reset( entry.data.substring( pos ) );
         while ( regxFeatureName.find() ) {
            final String base = regxFeatureName.group( 2 ).toLowerCase();
            String name = Arrays.stream( base.split( "(?<= )" ) ).map( Utils::ucfirst ).collect( Collectors.joining() );
            if ( base.equals( "hybrid talent options" ) || features.contains( name ) ) continue;
            // Add anchor
            String id = base.replace( ' ', '-' );
            aData = aData.replace( regxFeatureName.group(), "<br><br><b id="+id+">" + regxFeatureName.group(1) + name );
            features.add( name );
            // Correct case (A feature may repeat a few times)
            if ( ! name.equals( regxFeatureName.group( 2 ) ) ) {
               aData = aData.replaceAll( regxFeatureName.group( 2 ), name );
               corrections.add( "formatting" );
            }
         }
         entry.data = aData;
      } else {
         log.log( Level.WARNING, "Class features not found: {0} {1}", new Object[]{ entry.shortid, entry.name });
      }
   }
}