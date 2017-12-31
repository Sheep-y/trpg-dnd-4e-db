package db4e;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import sheepy.util.ResourceUtils;
import sheepy.util.Utils;

/**
 * Setup logging, create preference, and launch the application.
 */
public class Main {

   static {
      // Set log format and disable global logger
      System.setProperty( "java.util.logging.SimpleFormatter.format",  "%1$tT [%4$s] %5$s%6$s%n" );
      Logger.getLogger( "" ).getHandlers()[0].setLevel( Level.OFF );
   }

   static String TITLE = "Compendium downloader";
   static String VERSION = "3.6.1";
   static String UPDATE_TIME = "2018-01-01"; // Any release beyond this time is an update

   // Global log ang preference
   public static final Logger log = Logger.getLogger( Main.class.getName() );
   static final Preferences prefs = Preferences.userNodeForPackage( Main.class );
   public static final AtomicBoolean debug = new AtomicBoolean( false );
   public static final AtomicBoolean simulate = new AtomicBoolean( false ); // Simulate data download without getting real data.

   // Main method. No need to check java version because min version is compile target.
   public static void main( String[] args ) {
      if ( simulate.get() && ! VERSION.contains( "(development)" ) )
         simulate.set( false );
      log.setLevel( Level.CONFIG );
      try {
         Class.forName( "javafx.stage.Stage" ); // OpenJDK does not come with JavaFX by default
         MainApp.run( args );
      } catch  ( ClassNotFoundException ex ) {
         final String ERR = "This program requires JavaFX (or OpenJFX with WebKit) to run.";
         System.out.println( ERR );
         JOptionPane.showMessageDialog( null, ERR, TITLE + " " + VERSION, JOptionPane.ERROR_MESSAGE );
      }
   }

   public static CompletableFuture<Optional<Boolean>> checkUpdate ( boolean forceCheck ) {
      if ( ! forceCheck ) {
         try {
            Instant nextCheck = Instant.parse( prefs.get( "app.check_update" , Instant.now().toString() ) );
            if ( nextCheck.isAfter( Instant.now() ) ) {
               log.log( Level.INFO, "Skipping update check. Next check at {0}", nextCheck );
               return CompletableFuture.completedFuture( Optional.empty() );
            }
         } catch ( DateTimeParseException ex ) { }
      }

      CompletableFuture<Optional<Boolean>> result = new CompletableFuture<>();
      ForkJoinPool.commonPool().execute( () -> { try {
         URL url = new URL( "https://api.github.com/repos/Sheep-y/trpg-dnd-4e-db/releases/latest" );
         log.log( Level.FINE, "Checking update from {0}", url );
         String txt = ResourceUtils.getText( url.openStream() ), lastCreated = "0000";
         Matcher regxCreated = Pattern.compile( "\"created_at\"\\s*:\\s*\"([^\"]+)\"" ).matcher( txt );
         while ( regxCreated.find() )
            if ( regxCreated.group( 1 ).compareTo( lastCreated ) > 0 )
               lastCreated = regxCreated.group( 1 );
         prefs.put( "app.check_update", Instant.now().plus( 7, ChronoUnit.DAYS ).toString() );
         if ( lastCreated.equals( "0000" ) ) throw new DateTimeParseException( "Datetime not found on github release api.", txt, 0 );

         log.log( Level.INFO, "Checked update. Lastest release is {0}. Current {1}", new Object[]{ lastCreated, Main.UPDATE_TIME } );
         result.complete( Optional.of( lastCreated.compareTo( Main.UPDATE_TIME ) > 0 ) );
      } catch ( Exception e ) {
         log.log( Level.INFO, "Cannot check update: {0}", Utils.stacktrace( e ) );
         result.completeExceptionally( e );
      } } );
      return result;
   }

   public static void doUpdate () { try {
      Desktop.getDesktop().browse( new URI( "https://github.com/Sheep-y/trpg-dnd-4e-db/releases/latest" ) );
      } catch ( URISyntaxException | IOException e ) { }
   }
}