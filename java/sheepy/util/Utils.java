package sheepy.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.TimerTask;

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

}