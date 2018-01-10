package sheepy.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Utils {

   /** Return a copy of the source cloned in a sync block */
   public static <T> List<T> sync( List<T> source ) {
      return sync( source, source );
   }

   /** Return a copy of the source cloned in a sync block */
   public static <T> List<T> sync( List<T> source, Object lock ) {
      synchronized (lock) {
         return new ArrayList<>(source);
      }
   }

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      ex.printStackTrace( new PrintWriter( sw ) );
      return sw.toString();
   }

   public static List<String> matchAll ( Matcher m, String src ) {
      return matchAll( m, src, 0 );
   }

   public static List<String> matchAll ( Matcher m, String src, int group ) {
      List<String> result = new ArrayList<>();
      m.reset( src );
      while ( m.find() ) result.add( m.group( group ) );
      return result;
   }

   public static String ucfirst ( String text ) {
      if ( ! Character.isLowerCase( text.charAt( 0 ) ) ) return text;
      char[] data = text.toCharArray();
      data[0] = Character.toUpperCase( text.charAt( 0 ) );
      return new String( data );
   }

}