package db4e;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.prefs.Preferences;

public class Utils {

   public static String getMortalPref ( Preferences prefs, String key ) {
      String result = prefs.get( key, null );
      if ( result != null ) {
         String expireDate = prefs.get( key + ".expires", null );
         try {
            if ( OffsetDateTime.parse( expireDate ).isBefore( OffsetDateTime.now() ) )
               result = null;
         } catch ( NullPointerException | DateTimeParseException ex ) {
            result = null;
         }
      }
      if ( result == null ) {
         prefs.remove( key );
         prefs.remove( key + ".expires" );
      }
      return result;
   }

   public static void putMortalPref ( Preferences prefs, String key, String value, TemporalAmount expires ) {
      prefs.put( key, value );
      prefs.put( key + ".expires", OffsetDateTime.now().plus( expires ).toString() );
   }

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace( pw );
      return sw.toString();
   }

}
