package sheepy.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace( pw );
      return sw.toString();
   }

}