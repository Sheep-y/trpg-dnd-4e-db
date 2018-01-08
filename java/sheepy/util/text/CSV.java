package sheepy.util.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSV {

   private static final String csvTokenPattern = "(?<=^|,)([^\"\\r\\n,]*+|\"(?:\"\"|[^\"]++)*\")(?:,|$)";
   private static final Matcher csvToken = Pattern.compile( csvTokenPattern ).matcher( "" );
   private static final List<String> csvBuffer = new ArrayList<>();

   public static synchronized String[] parseCsvLine ( CharSequence line ) { return parseCsvLine( line, true ); }
   public static synchronized String[] parseCsvLine ( CharSequence line, boolean ignoreError ) {
      csvToken.reset( line );
      csvBuffer.clear();
      int pos = 0;
      while ( csvToken.find() ) {
         if ( ! ignoreError && csvToken.start() != pos )
            throw new IllegalArgumentException( "Malformed csv line: " + line );
         String token = csvToken.group( 1 );
         if ( token.length() >= 2 && token.charAt(0) == '"' && token.endsWith( "\"" ) )
            token = token.substring( 1, token.length()-1 ).replaceAll( "\"\"", "\"" );
         csvBuffer.add(token);
         pos = csvToken.end();
      }
      if ( ! ignoreError && pos != line.length() )
         throw new IllegalArgumentException( "Data after csv line: " + line );
      return csvBuffer.toArray( new String[ csvBuffer.size() ] );
   }

   public static final Matcher csvQuotable = Pattern.compile( "[\r\n,\"]" ).matcher( "" );

   public static synchronized StringBuilder buildCsvLine ( Object[] line ) {
      StringBuilder result = new StringBuilder(32);
      for ( Object field : line ) {
         String token = field.toString();
         if ( csvQuotable.reset( token ).find() )
            result.append( '"' ).append( token.replaceAll( "\"", "\"\"" ) ).append( "\"," );
         else
            result.append( token ).append( ',' );
      }
      result.setLength( result.length() - 1 );
      return result;
   }
}