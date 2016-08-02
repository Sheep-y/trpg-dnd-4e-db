package db4e.controller;

import db4e.Main;
import db4e.SceneMain;
import db4e.convertor.Convertor;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.web.WebEngine;
import javax.security.auth.login.LoginException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import netscape.javascript.JSException;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.w3c.dom.Document;
import sheepy.util.Utils;
import sheepy.util.ui.ConsoleWebView;
import sheepy.util.ui.ObservableArrayList;

/**
 * Data Management.  Both download and export is controlled from a single thread.
 *
 * "this" is used to lock db, dal, and thread operations.
 * "categories" is used to lock data access, including downloadComplete flag.
 */
public class Controller {

   private static final Logger log = Main.log;

   public static final int MIN_TIMEOUT_MS = 10_000;
   public static final int MIN_INTERVAL_MS = 0;
   public static final int DEF_TIMEOUT_MS = 30_000;
   public static final int DEF_INTERVAL_MS = 1_000;
   public static final int DEF_RETRY_COUNT = 5;

   public static volatile int TIMEOUT_MS = DEF_TIMEOUT_MS;
   public static volatile int INTERVAL_MS = DEF_INTERVAL_MS;
   public static volatile int RETRY_COUNT = DEF_RETRY_COUNT;

   public static final String DB_NAME = "dnd4_compendium.database";

   // Database variables are set on open().
   private volatile SqlJetDb db;
   private volatile DbAbstraction dal;
   private Thread currentThread;
   private final ProgressState state;
   private boolean hasReset = false;

   public final ObservableList<Category> categories = new ObservableArrayList<>();

   private final SceneMain gui;
   private final ConsoleWebView browser;
   private final WebEngine engine;
   private final Crawler crawler;
   private final Exporter exporter;
   private final Timer scheduler = new Timer();
   private final ForkJoinPool threadPool = ForkJoinPool.commonPool(); // Only need one thread

   public Controller ( SceneMain main ) {
      gui = main;
      browser = main.getWorker();
      engine = browser.getWebEngine();
      crawler = new Crawler( engine );
      exporter = new Exporter();
      state = new ProgressState( ( progress ) -> {
         checkStop( null );
         main.setProgress( progress );
      } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Stop task
   /////////////////////////////////////////////////////////////////////////////

   public void stop () {
      engine.getLoadWorker().cancel();
      synchronized ( this ) {
         if ( currentThread != null )
            currentThread.interrupt();
      }
   }

   private void checkStop ( String status ) {
      checkStop( status, null );
   }
   private void checkStop ( String status, Double progress ) {
      if ( status != null ) gui.setStatus( status );
      if ( progress != null ) gui.setProgress( progress );
      synchronized ( this ) {
         assert( currentThread == Thread.currentThread() );
         if ( Thread.interrupted() )
            throw new RuntimeException( new InterruptedException() );
      }
   }

   /**
    * Return a function that is used to clean up running task and perhaps display error message
    *
    * @param action Action name (noun) to update gui status
    * @param enabler Called when any error happened
    * @return A cleanup function that accepts second parameter as an error (can be null).
    */
   private BiConsumer<Void,Throwable> terminate ( String action, Consumer<String> enabler ) {
      return ( result, err ) -> {
         if ( err != null ) {
            if ( err instanceof CompletionException && err.getCause() != null )
               err = err.getCause();
            while ( ( err instanceof RuntimeException || err instanceof ExecutionException ) && err.getCause() != null )
               err = err.getCause(); // Unwrap RuntimeException and ExecutionExceptiom

            if ( err instanceof InterruptedException ) {
               enabler.accept( action + " stopped" );
               gui.setTitle( "Stopped" );
            } else if ( err instanceof TimeoutException ) {
               enabler.accept( action + " timeout" );
               gui.setTitle( "Timeout" );
            } else if ( err instanceof JSException ) {
               enabler.accept( action + " failed (script error)" );
               gui.setTitle( "Error" );
            } else {
               gui.setTitle( "Error" );
               log.log( Level.WARNING, action + " failed: {0}", Utils.stacktrace( err ) );
               String msg = ( (Throwable) err ).getMessage();
               if ( msg == null || msg.isEmpty() ) msg = err.toString();
               if ( msg.contains( "Exception: ") ) msg = "Error: " + msg.split( "Exception: ", 2 )[1];
               enabler.accept( msg );
               if ( err instanceof LoginException )
                  gui.focusUsername();
            }
         } else {
            gui.setTitle( "Done" );
         }
         state.update();
         currentThread = null;
         Thread.interrupted(); // Clear flag
      };
   }

   /////////////////////////////////////////////////////////////////////////////
   // Open, Close, and Reset
   /////////////////////////////////////////////////////////////////////////////

   public void resetDb () {
      gui.setStatus( "Clearing data" );
      gui.setProgress( -1.0 );
      synchronized ( categories ) {
         categories.clear();
      }
      state.done = state.total = 0;
      hasReset = true;

      threadPool.execute( () -> { try {
         synchronized ( this ) { // Lock database for the whole duration
            closeDb();
            Thread.sleep( 1000 ); // Give it some time to save and close
            final File file = new File( DB_NAME );
            if ( file.exists() ) {
               log.log( Level.INFO, "Deleting database {0}", new File( DB_NAME ).getAbsolutePath() );
               file.delete();
            } else {
               log.log( Level.WARNING, "Database file not found: {0}", new File( DB_NAME ).getAbsolutePath() );
            }
            Thread.sleep( 500 ); // Give OS some time to delete the file
            open( null );
         }

      } catch ( Exception ex ) {
         log.log( Level.WARNING, "Error when deleting database: {0}", Utils.stacktrace( ex ) );
         open( null ).whenComplete( ( a, b ) -> gui.setStatus( "Cannot clear data" ) );

      } } );
   }

   public CompletableFuture<Void> open ( TableView categoryTable ) {
      gui.stateBusy( "Opening database" );
      return CompletableFuture.runAsync( () -> {
         File db_file = new File( DB_NAME );
         String db_path = db_file.getAbsolutePath();
         try {
            log.log( Level.INFO, "Opening local database {0}", db_path );
            synchronized ( this ) {
               db = SqlJetDb.open( db_file, true );
               dal = new DbAbstraction();
            }
         } catch ( Exception err ) {
            log.log(Level.SEVERE, "Cannot open local database {0}: {1}", new Object[]{ db_path, Utils.stacktrace( err ) } );

            db_file = new File( System.getProperty("user.home") + "/" + DB_NAME );
            db_path = db_file.getAbsolutePath();
            try {
               log.log( Level.INFO, "Opening user database {0}", db_path );
               synchronized ( this ) {
                  db = SqlJetDb.open( db_file, true );
                  dal = new DbAbstraction();
               }
            } catch ( Exception ex ) {
               log.log( Level.SEVERE, "Cannot open user database {0}: {1}", new Object[]{ db_path, Utils.stacktrace( ex ) } );
               gui.stateBadData();
               closeDb();
               throw new RuntimeException( ex );
            }
         }

         log.log( Level.CONFIG, "Opened database {0}", db_file );
         if ( categoryTable != null ) Platform.runLater( () -> {
            categoryTable.setItems( categories );
         } );
         openOrCreateTable();
      } );
   }

   public void close() {
      stop();
      scheduler.cancel();
      closeDb();
   }

   private synchronized void closeDb () {
      if ( db != null ) try {
         log.log( Level.FINE, "Closing database" );
         db.close();
         db = null;
         dal = null;
      } catch ( Exception ex ) {
         log.log( Level.WARNING, "Error when closing database: {0}", Utils.stacktrace( ex ) );
      }
   }

   private void openOrCreateTable () {
      gui.setStatus( "Reading data" );
      try {
         synchronized ( categories ) {
            dal.setDb( db, categories, state );
         }

      } catch ( Exception e1 ) {

         log.log( Level.CONFIG, "Create tables because {0}", Utils.stacktrace( e1 ) );
         try {
            dal.createTables();
            synchronized ( categories ) {
               dal.setDb( db, categories, state );
            }

         } catch ( Exception e2 ) {
            log.log( Level.SEVERE, "Cannot create tables: {0}", Utils.stacktrace( e2 ) );
            gui.stateBadData();
            closeDb();
            throw new RuntimeException( e2 );
         }
      }

      backupDb();

      final boolean downloadIncomplete = categories.stream().anyMatch( e -> e.downloaded_entry.get() <= 0 );
      if ( downloadIncomplete ) {
         gui.stateCanDownload( "Ready to download" );
         if ( state.done <= 0 && ! hasReset && gui.getUsername().isEmpty() && gui.getPassword().isEmpty() )
            gui.selectTab( "help" );
      } else
         gui.stateCanExport( "Ready to export" );
      state.update();
   }

   /**
    * If current database is bigger than last backup, update the backup.
    */
   private void backupDb () {
      File current = db.getFile();
      File backup = new File( current.getPath() + ".backup" );
      final long currentSize = current.length();
      final long backupSize = backup.length();
      if ( currentSize <= backupSize || currentSize <= 12000 ) {
         log.log( Level.INFO, "No need to back up {0} ({1} <= {2} or 12k)", new Object[]{ current, currentSize, backupSize } );
         return;
      }
      threadPool.execute( () -> { try {
         log.log( Level.INFO, "Backing up {0} ({1} > {2})", new Object[]{ current, currentSize, backupSize } );
         Files.copy( current.toPath(), backup.toPath(), REPLACE_EXISTING );
         log.log( Level.FINE, "Created backup {0}", backup );
      } catch ( Exception e ) {
         log.log( Level.WARNING, "Cannot create backup {0}: {1}", new Object[]{ backup, Utils.stacktrace( e ) } );
      } } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Download
   /////////////////////////////////////////////////////////////////////////////

   // Open compendium
   public CompletableFuture<Void> startDownload () {
      gui.setTitle( "Downloading" );
      gui.stateRunning();
      gui.setProgress( -1.0 );
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );
      log.log( Level.CONFIG, "Timeout {0} ms / Interval {1} ms ", new Object[]{ TIMEOUT_MS, INTERVAL_MS } );
      return runTask( () -> {
         setPriority( Thread.NORM_PRIORITY );
         if ( categories.stream().anyMatch( e -> e.total_entry.get() <= 0 ) )
            runAndCheckLogin( "Testing login", crawler::randomGlossary );
         downloadCategory();
         downloadEntities();
         gui.stateCanExport( "Download complete, may export data" );
      } ).whenComplete( terminate( "Download", gui::stateCanDownload ) );
   }

   private void downloadCategory() throws Exception { // Too many exceptions to throw one by one
      TransformerFactory factory = null;

      for ( Category category : categories ) {
         if ( category.total_entry.get() > 0 ) continue;
         String name = category.name.toLowerCase();

         runAndGet( "Getting " + name + " template", () ->
            crawler.getCategoryXsl( category ) );
         Document xsl = engine.getDocument();

         runAndGet( "Getting " + name + " data", () ->
            crawler.getCategoryData( category ) );
         Document xml = engine.getDocument();

         checkStop( "Parsing " + name );
         if ( factory == null ) factory = TransformerFactory.newInstance();
         StringWriter result = new StringWriter();
         factory.newTransformer( new DOMSource( xsl ) ).transform( new DOMSource( xml ), new StreamResult( result ) );

         runAndGet( "Listing " + name, () -> Platform.runLater( () ->
            engine.loadContent( result.toString() ) ) );
         List<Entry> entries = crawler.openCategory();

         checkStop( "Saving " + name );
         dal.saveEntryList( category, entries );

         checkStop( "Listed " + name );
      }
      state.total = categories.stream().mapToInt( c -> c.total_entry.get() ).sum();
      state.update();
   }

   private void downloadEntities () throws Exception {
      Instant[] pastFinishTime = new Instant[ 64 ]; // Past 64 finish time
      int remainingCount = state.total - state.done, second;
      for ( Category category : categories ) {
         for ( Entry entry : category.entries ) {
            if ( ! entry.contentDownloaded ) {
               String name = entry.name + " (" + category.name + ")";
               runAndCheckLogin( name, () -> crawler.openEntry( entry ) );
               crawler.getEntry( entry );
               dal.saveEntry( entry );

               category.downloaded_entry.set( category.downloaded_entry.get() + 1 );
               state.addOne();

               --remainingCount;
               if ( remainingCount > 0 ) {
                  System.arraycopy( pastFinishTime, 1, pastFinishTime, 0, 63 );
                  pastFinishTime[63] = Instant.now();
                  if ( pastFinishTime[56] == null ) continue;
                  // Remaining time
                  if ( pastFinishTime[0] != null ) { // Use all 64 entry to estimate time
                     Duration sessionTime = Duration.between( pastFinishTime[0], Instant.now() );
                     second = (int) Math.ceil( ( sessionTime.getSeconds() / (double) 64 ) * remainingCount );
                  } else { // Use 8 entry to estimate time
                     Duration sessionTime = Duration.between( pastFinishTime[56], Instant.now() );
                     second = (int) Math.ceil( ( sessionTime.getSeconds() / (double) 8 ) * remainingCount );
                  }
                  // And make sure it's not less than current interval
                  second = Math.max( second, (int) Math.ceil( remainingCount * (double) INTERVAL_MS / 1000 ) );
                  // Manual format and display
                  if ( second >= 86400 )    gui.setTitle( ( second / 86400 ) + "d " + ( ( second % 86400 ) / 3600 ) + "h remain" );
                  if ( second >= 3600 )     gui.setTitle( ( second / 3600 ) + "h " + ( ( second % 3600 ) / 60 )  + "m remain" );
                  else if ( second >= 100 ) gui.setTitle( ( second / 60 )  + "m remain" );
                  else                      gui.setTitle( second + "s remain" );
               }
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Export
   /////////////////////////////////////////////////////////////////////////////

   public CompletableFuture<Void> startExport ( File target ) {
      gui.setTitle( "Export" );
      gui.stateRunning();
      gui.setProgress( -1.0 );
      state.done = 0;
      log.log( Level.CONFIG, "Export target: {0}", target );
      return runTask( () -> {
         // Export process is mainly IO limited. Not wasting time to make it multi-thread.
         try {
            exporter.testViewerExists();
         } catch ( IOException ex ) {
            throw new FileNotFoundException( "No viewer. Run ant make." );
         }

         setPriority( Thread.MIN_PRIORITY );
         String root = target.getPath().replaceFirst( "\\.html?$", "_files/" );
         log.log( Level.CONFIG, "Export root: {0}", target );
         new File( root ).mkdirs();

         Convertor.beforeConvert( categories );

         checkStop( "Writing main catlog" );
         exporter.writeCatalog( root, categories );

         checkStop( "Loading content" );
         dal.loadEntityContent( categories, state );
         convertDataForExport();
         Convertor.afterConvert();

         checkStop( "Writing data" );
         state.done = 0;
         state.update();
         for ( Category category : categories ) {
            if ( category.meta != null ) {
               log.log( Level.FINE, "Writing {0}", category.id );
               exporter.writeCategory( root, category, state );
            }
         }

         checkStop( "Writing viewer" );
         exporter.writeViewer( target );

         gui.stateCanExport( "Export complete, may view data" );
      } ).whenComplete( terminate( "Export", gui::stateCanExport ) );
   }

   private void convertDataForExport () throws InterruptedException, ExecutionException {
      checkStop( "Converting" );
      state.done = 0;
      state.update();
      try {
         Convertor.stop.set( false );
         List<CompletableFuture<Void>> tasks = new ArrayList<>( categories.size() );
         for ( Category category : categories ) {
            Convertor convertor = Convertor.getConvertor( category, gui.isDebugging() );
            if ( convertor != null )
               tasks.add( convertor.convert( state, threadPool ) );
         }
         CompletableFuture.allOf( tasks.toArray( new CompletableFuture[ tasks.size() ] ) ).get();
      } catch ( Exception e ) {
         Convertor.stop.set( true );
         throw e;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Shared / Utils
   /////////////////////////////////////////////////////////////////////////////

   private CompletableFuture<Void> runTask ( RunExcept task ) {
      final CompletableFuture<Void> result = new CompletableFuture<>();
      threadPool.execute( ()-> { try {
         synchronized ( this ) { currentThread = Thread.currentThread(); } // Unset (cleanup) by terminate()
         synchronized ( categories ) { // Sync data
            task.run();
         }
         result.complete( null );

      } catch ( Exception e ) {
         result.completeExceptionally( e );
      } } );
      return result;
   }

   private void setPriority ( int priority ) { try {
      Thread.currentThread().setPriority( priority );
   } catch ( SecurityException ignored ) {} }

   /**
    * Run a browsing task and check whether it request login.
    * If so, it will get credentials from main window and try to login.
    * If login is successful, it will rerun the task,
    * otherwise it throws LoginException.
    *
    * @param taskName Name to display on gui status
    * @param task Task to run.  Must change browser document.
    * @throws Exception InterruptedException and LoginException are most common.
    */
   private void runAndCheckLogin ( String taskName, RunExcept task ) throws Exception {
      runAndGet( taskName, task );
      if ( crawler.needLogin() ) {
         log.log( Level.INFO, "Requires login: {0}", engine.getLocation() );
         runAndGet( "Opening login page", crawler::openLoginPage );
         runAndGet( "Logging in", () -> crawler.login( gui.getUsername(), gui.getPassword() ) );
         // Post login page may contain forms (e.g. locate a store), so rerun task before check
         runAndGet( taskName, task );
         if ( crawler.needLogin() ) {
            log.log( Level.INFO, "Login failed: {0}", engine.getLocation() );
            throw new LoginException( "Login incorrect or expired, see Help." );
         }
      }
   }

   private Instant lastLoad = Instant.now();

   /**
    * Call a task and wait for browser to finish loading... or timeout.
    * The task must cause the browser's loader to change state for this to work.
    *
    * @param taskName Name of task, used in logging and timeout message.
    * @param task Task to run.
    */
   private void runAndGet ( String taskName, RunExcept task ) {
      final CompletableFuture<Void> future = new CompletableFuture<>();
      final AtomicInteger retry = new AtomicInteger();

      do {
         try {
            if ( INTERVAL_MS > 0 ) {
               lastLoad = lastLoad.plusMillis( INTERVAL_MS );
               long sleep = Duration.between( lastLoad, Instant.now() ).toMillis();
               if ( sleep < INTERVAL_MS ) Thread.sleep( INTERVAL_MS - sleep );
            }
            lastLoad = Instant.now();
            Platform.runLater( browser.getConsoleOutput()::clear );
            checkStop( taskName );
            browser.handle( ( e ) -> future.complete( null ), // on load
                        ( e,err ) -> future.completeExceptionally( err ) ); // on error
            task.run();
            future.get( TIMEOUT_MS, TimeUnit.MILLISECONDS );
            browser.handle( null, null );
            log.log(Level.FINE, "{0} finished normally.", taskName);
            break;

         } catch ( Exception err ) {
            browser.handle( null, null );
            log.log( Level.WARNING, "{0} finished exceptionally: {1}", new Object[]{ taskName, err } );

            if ( err instanceof TimeoutException && retry.incrementAndGet() <= RETRY_COUNT ) {
               int sleep_sec = retry.get() > 3 ? 300 : new int[]{ 0, 10, 60, 120 }[ retry.get() ];
               checkStop( "Timeout, waiting " + sleep_sec + " seconds before retry" );
               try {
                  Thread.sleep( sleep_sec * 1000 );
               } catch ( InterruptedException ex ) {
                  throw new RuntimeException( ex );
               }
            } else
               throw new RuntimeException( err );
         }
      } while ( true );
   }

   /**
    * Same as Runnable, but throws Exception.
    */
   private static interface RunExcept {
      void run ( ) throws Exception;
   }
}