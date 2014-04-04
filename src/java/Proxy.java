// Proxy.java - applet to write to file / cross origin request
import java.io.*;
import java.net.URL;
import java.security.*;
import javax.swing.JApplet;

public class Proxy extends JApplet {
   public String write(String f, String txt) {
      return write(f, txt, "UTF-8" );
   }

   public String write(final String f, final String txt, final String charset) {
      Throwable e;
      // Error: local variable referenced from inner class must be final. Can work around by creating a method but perhaps not worth the code.
      // Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { @Override public void uncaughtException(Thread t, Throwable ex) { e = ex; }});
      URL base = getDocumentBase();
      if ( ! base.getProtocol().equals( "file" ) ) return "Cannot write to protocol " + base.getProtocol();
      final String name = base.toString().replaceAll( "^file:/+|/[^/]*$", "" ) + "/" + f;
      try {
         e = AccessController.doPrivileged( new PrivilegedAction<Throwable>(){ @Override public Throwable run() {
            // getDocumentBase = file:///C:/somewhere/subfolder/file.ext
            try ( Writer w = new OutputStreamWriter( new FileOutputStream(f), charset ) ) {
               w.write(txt);
            } catch (Exception ex) { return ex; }
            return null;
         }} );
      } catch (Exception ex) { e = ex; }
      if ( e != null ) {
         e.printStackTrace();
         return e.getClass() + ": " + e.getMessage() + " (" + name + "@" + charset + ")";
      }
      return null;
   }
}