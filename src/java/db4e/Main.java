package db4e;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import sheepy.util.JavaFX;

public class Main extends Application {

   static {
      // Set log format and disable global logger
      System.setProperty( "java.util.logging.SimpleFormatter.format",  "%1$tT [%4$s] %5$s%6$s%n" );
      Logger.getLogger( "" ).getHandlers()[0].setLevel( Level.OFF );
   }

   // Main method.  Do virtually nothing.
   public static void main( String[] args ) { 
      launch( args ); 
   }

   // System utilities
   public static final Logger log = Logger.getLogger( Main.class.getName() );
   public static final Preferences prefs = Preferences.userNodeForPackage( Main.class );

   @Override public void start( Stage stage ) throws Exception {
      stage.setScene( new SceneMain( this ) );
      stage.show();
      log.info( "UI Layout initialised" );
   }

   public void addLoggerOutput( TextInputControl textInput ) throws SecurityException {
      // Setup our logger which goes to log tab.
      Handler handler = new Handler() {
         private volatile boolean closed = false;
         @Override public void publish( LogRecord record ) {
            if ( closed ) return;
            JavaFX.appendText( textInput, getFormatter().format( record ) );
         }
         @Override public void flush() {}
         @Override public void close() throws SecurityException { closed = true; }
      };
      handler.setFormatter( new SimpleFormatter() );
      log.addHandler( handler );
   }
}