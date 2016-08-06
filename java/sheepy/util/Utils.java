package sheepy.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class Utils {

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace( pw );
      return sw.toString();
   }

   public static TimerTask toTimer ( Runnable task ) {
      return new TimerTask() {
         @Override public void run() {
            task.run();
         }
      };
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
      return Character.toUpperCase( text.charAt( 0 ) ) + text.substring(1);
   }
}