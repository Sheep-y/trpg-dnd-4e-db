package db4e;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.prefs.Preferences;
import sheepy.util.text.I18n;

public class Utils {

   public static String getMortalPref ( Preferences prefs, String key ) {
      String result;
      synchronized( prefs ) {
         result = prefs.get( key, null );
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
      }
      return result;
   }

   public static void putMortalPref ( Preferences prefs, String key, String value, TemporalAmount expires ) {
      synchronized ( prefs ) {
         prefs.put( key, value );
         prefs.put( key + ".expires", OffsetDateTime.now().plus( expires ).toString() );
      }
   }

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace( pw );
      return sw.toString();
   }

   private final static CharsetDecoder utf8  = I18n.strictDecoder( I18n.UTF8 );
   private final static CharsetDecoder utf16 = I18n.strictDecoder( I18n.UTF16 );

   /**
    * Load a Unicode file as String.
    * @param file File to read
    * @return File content in string
    * @throws IOException If file cannot be read
    * @throws CharacterCodingException If file is not utf-8 or utf-16.
    */
   public static String loadFile ( File file ) throws IOException {
      byte[] bytes = Files.readAllBytes( file.toPath() );
      try {
         return I18n.decode( bytes, utf8 );
      } catch ( CharacterCodingException ex ) {
         return I18n.decode( bytes, utf16 );
      }
   }
}