// Proxy.java - applet to write to file / cross origin request
import java.io.*;
import java.security.*;
import java.nio.charset.Charset;

public class Proxy extends java.applet.Applet {
   public String test() { return "1"; }

   public void write(String f, String txt) {
      write(f, txt, "UTF-8" );
   }

   public Throwable write(final String f, final String txt, final String code) {
      Exception e = null;
      //Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { @Override public void uncaughtException(Thread t, Throwable ex) { e = ex; }});
      try {
         e = AccessController.doPrivileged( new PrivilegedAction<Exception>(){ @Override public Exception run() {
            try ( Writer w = new OutputStreamWriter( new FileOutputStream(f), Charset.forName(code) ) ) {
               w.write(txt);
            } catch (Exception ex) { return ex; }
            return null;
         }} );
      } catch (Exception ex) { e = ex; }
      return e;
   }
}