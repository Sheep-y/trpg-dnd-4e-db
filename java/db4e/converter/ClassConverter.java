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
      featureMap = new HashMap<>( category.entries.size(), 1f );
   }

   private final Matcher regxFeatureName  = Pattern.compile( "<br><br><b>(Level \\d+ )?([^<(]+?)(?=\\s*[<(])" ).matcher( "" );
   private final Map<String, Set<String>> featureMap;

   @Override protected void convertEntry( Entry entry ) {
      super.convertEntry( entry );
      int pos = entry.data.indexOf( " FEATURE" );
      if ( pos > 0 ) {
         Set<String> features = featureMap.get( entry.shortid );
         if ( features == null ) featureMap.put( entry.shortid, features = new HashSet<>() );
         regxFeatureName.reset( entry.data.substring( pos ) );
         while ( regxFeatureName.find() ) {
            final String base = regxFeatureName.group( 2 ).toLowerCase();
            String name = Arrays.stream( base.split( "(?<=[( ])" ) ).map( Utils::ucfirst ).collect( Collectors.joining() );
            if ( base.equals( "hybrid talent options" ) || features.contains( name ) ) continue;
            features.add( name );
            if ( ! name.equals( regxFeatureName.group( 2 ) ) )
               corrections.add( "formatting" );
            System.out.println(entry.name + ": " + name  );
         }
      } else {
         log.log( Level.WARNING, "Class features not found: {0} {1}", new Object[]{ entry.shortid, entry.name });
      }
   }
}