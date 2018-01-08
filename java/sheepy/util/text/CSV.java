package sheepy.util.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSV {

   public static class CsvParser {

      private final Matcher csvToken = Pattern.compile( "(?<=^|,)([^\"\\r\\n,]*+|\"(?:\"\"|[^\"]++)*\")(?:,|$)" ).matcher( "" );
      private final List<String> csvBuffer = new ArrayList<>();

      public String[] parseCsvLine ( CharSequence line ) { return parseCsvLine( line, false ); }
      public synchronized String[] parseCsvLine ( CharSequence line, boolean checkError ) {
         csvToken.reset( line );
         int pos = 0;
         while ( csvToken.find() ) {
            if ( checkError && csvToken.start() != pos )
               throw new IllegalArgumentException( "Malformed csv line: " + line );
            String token = csvToken.group( 1 );
            if ( token.length() >= 2 && token.charAt(0) == '"' && token.endsWith( "\"" ) )
               token = token.substring( 1, token.length()-1 ).replaceAll( "\"\"", "\"" );
            csvBuffer.add(token);
            pos = csvToken.end();
         }
         if ( checkError && pos != line.length() )
            throw new IllegalArgumentException( "Data after csv line: " + line );
         String[] result = csvBuffer.toArray( new String[ csvBuffer.size() ] );
         csvBuffer.clear();
         return result;
      }
   }

   public static class CsvBuilder {

      public final Matcher csvQuotable = Pattern.compile( "[\r\n,\"]" ).matcher( "" );

      public synchronized StringBuilder buildCsvLine ( Object[] line ) {
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
}