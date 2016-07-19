package db4e;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javax.swing.JOptionPane;
import sheepy.util.JavaFX;
import sheepy.util.ResourceUtils;
import sheepy.util.Utils;

/**
 * Setup logging, load preference, and show downloader main GUI.
 */
public class Main extends Application {

   static {
      // Set log format and disable global logger
      System.setProperty( "java.util.logging.SimpleFormatter.format",  "%1$tT [%4$s] %5$s%6$s%n" );
      Logger.getLogger( "" ).getHandlers()[0].setLevel( Level.OFF );
   }

   static String TITLE = "Compendium downloader";

   // Global log ang preference
   public static final Logger log = Logger.getLogger( Main.class.getName() );
   static final Preferences prefs = Preferences.userNodeForPackage( Main.class );

   // Main method.  Do virtually nothing.
   public static void main( String[] args ) {
      try {
         Class.forName( "java.util.concurrent.CompletableFuture" );
         launch( args );
      } catch ( ClassNotFoundException ex ) {
         JOptionPane.showMessageDialog( null, "Requires Java 1.8 or above", TITLE, JOptionPane.ERROR_MESSAGE );
         Platform.exit();
      }
   }

   @Override public void start( Stage stage ) throws Exception {
      final SceneMain sceneMain = new SceneMain( this );
      log.log( Level.CONFIG, "Java {0} on {1} {2}", new Object[]{ System.getProperty( "java.runtime.version" ), System.getProperty("os.name"), System.getProperty("os.arch") });

      stage.setTitle( TITLE );
      stage.setScene( sceneMain );
      try {
         stage.getIcons().add( new Image( ResourceUtils.getStream( "img/icon.png" ) ) );
      } catch ( Exception err ) {
         log.warning( Utils.stacktrace( err ) );
      }
      stage.setOnCloseRequest( e -> { try {
            sceneMain.shutdown();
            prefs.flush();
            for ( Handler handler : log.getHandlers() )
               handler.close();
         } catch ( BackingStoreException | SecurityException | NullPointerException ignored ) { } } );
      stage.show();

      log.info( "Main GUI initialised." );
      sceneMain.startup();
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