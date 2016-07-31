package db4e;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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
   static String VERSION = "3.5.1 (development)";
   static String UPDATE_TIME = "9999-99-99"; // Any release beyond this time is an update

   // Global log ang preference
   public static final Logger log = Logger.getLogger( Main.class.getName() );
   static final Preferences prefs = Preferences.userNodeForPackage( Main.class );

   // Main method. No need to check java version because min version is compile target.
   public static void main( String[] args ) {
      launch( args );
   }

   @Override public void start( Stage stage ) throws Exception {
      final SceneMain sceneMain = new SceneMain( this );
      log.log( Level.CONFIG, "Java {0} on {1} {2}", new Object[]{ System.getProperty( "java.runtime.version" ), System.getProperty("os.name"), System.getProperty("os.arch") });

      stage.setTitle( TITLE );
      stage.setScene( sceneMain );
      try {
         stage.getIcons().add( new Image( ResourceUtils.getStream( "res/icon.png" ) ) );
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
            String msg = getFormatter().format( record );
            JavaFX.appendText( textInput, msg );
            if ( record.getLevel().intValue() >= Level.WARNING.intValue() )
               System.err.print( msg );
         }
         @Override public void flush() {}
         @Override public void close() throws SecurityException { closed = true; }
      };
      handler.setFormatter( new SimpleFormatter() );
      log.addHandler( handler );
   }

   public static CompletableFuture<Boolean> checkUpdate ( boolean forceCheck ) {
      if ( ! forceCheck ) {
         try {
            Instant nextCheck = Instant.parse( prefs.get( "app.check_update" , Instant.now().toString() ) );
            if ( nextCheck.isAfter( Instant.now() ) ) {
               log.log( Level.INFO, "Skipping update check. Next check at {0}", nextCheck );
               return CompletableFuture.completedFuture( false );
            }
         } catch ( DateTimeParseException ex ) { }
      }

      CompletableFuture<Boolean> result = new CompletableFuture<>();
      ForkJoinPool.commonPool().execute( () -> { try {
         URL url = new URL( "https://api.github.com/repos/Sheep-y/trpg-dnd-4e-db/releases/latest" );
         log.log( Level.FINE, "Checking update from {0}", url );
         String txt = ResourceUtils.getText( url.openStream() ), lastCreated = "0000";
         Matcher regxCreated = Pattern.compile( "\"created_at\"\\s*:\\s*\"([^\"]+)\"" ).matcher( txt );
         while ( regxCreated.find() )
            if ( regxCreated.group( 1 ).compareTo( lastCreated ) > 0 )
               lastCreated = regxCreated.group( 1 );
         prefs.put( "app.check_update", Instant.now().plus( 7, ChronoUnit.DAYS ).toString() );

         log.log( Level.INFO, "Checked update. Lastest release is {0}. Current {1}", new Object[]{ lastCreated, UPDATE_TIME } );
         result.complete( lastCreated.compareTo( UPDATE_TIME ) > 0 );
      } catch ( IOException e ) {
         log.log( Level.INFO, "Cannot check update: {0}", Utils.stacktrace( e ) );
      } } );
      return result;
   }

   public static void doUpdate () { try {
      Desktop.getDesktop().browse( new URI( "https://github.com/Sheep-y/trpg-dnd-4e-db/releases/latest" ) );
      } catch ( URISyntaxException | IOException e ) { }
   }
}