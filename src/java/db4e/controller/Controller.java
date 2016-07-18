package db4e.controller;

import db4e.Main;
import db4e.SceneMain;
import db4e.convertor.Convertor;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
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

   public static final int DEF_INTERVAL_MS = 1_000;
   public static final int MIN_TIMEOUT_MS = 10_000;
   public static final int MIN_INTERVAL_MS = 0;
   public static final int DEF_TIMEOUT_MS = 30_000;

   public static volatile int TIMEOUT_MS = 30_000;
   public static volatile int INTERVAL_MS = 1_000;

   public static final String DB_NAME = "dnd4_compendium.database";

   // Database variables are set on open().
   private volatile SqlJetDb db;
   private volatile DbAbstraction dal;
   private Thread currentThread;
   private ProgressState state;

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

   // Return a function that is used to clean up running task and perhaps display error message
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
            } else {
               gui.setTitle( "Error" );
               log.log( Level.WARNING, action + " failed: {0}", Utils.stacktrace( err ) );
               String msg = ( (Exception) err ).getMessage();
               if ( msg == null || msg.isEmpty() ) msg = err.toString();
               if ( msg.contains( "Exception: ") ) msg = msg.split( "Exception: ", 2 )[1];
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
         if ( categoryTable != null ) categoryTable.setItems( categories );
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

      if ( state.done < state.total || categories.stream().anyMatch( e -> e.total_entry.get() <= 0 ) )
         gui.stateCanDownload( "Ready to download" );
      else
         gui.stateCanExport( "Ready to export" );
      state.update();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Download
   /////////////////////////////////////////////////////////////////////////////

   // Open compendium
   public CompletableFuture<Void> startDownload () {
      gui.stateRunning();
      gui.setProgress( -1.0 );
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );
      log.log( Level.CONFIG, "Timeout {0} ms / Interval {1} ms ", new Object[]{ TIMEOUT_MS, INTERVAL_MS } );
      return runTask( () -> {
         runAndCheckLogin( "Connect compendium", crawler::randomGlossary );
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

         runAndGet( "Get " + name + " template", () ->
            crawler.getCategoryXsl( category ) );
         Document xsl = engine.getDocument();

         runAndGet( "Get " + name + " data", () ->
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
      Instant[] pastFinishTime = new Instant[ 10 ]; // Past 10 finish time
      int remainingCount = state.total - state.done;
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
                  System.arraycopy( pastFinishTime, 1, pastFinishTime, 0, 9 );
                  pastFinishTime[9] = Instant.now();
                  if ( pastFinishTime[0] == null ) continue;
                  // Remaining time
                  Duration sessionTime = Duration.between( pastFinishTime[0], Instant.now() );
                  int second = (int) Math.ceil( ( sessionTime.getSeconds() / (double) 10 ) * remainingCount );
                  // And make sure it's not less than current interval
                  second = Math.max( second, (int) Math.ceil( remainingCount * (double) INTERVAL_MS / 1000 ) );
                  gui.setTitle( Duration.ofSeconds(  second ).toString().substring( 2 ) + " remain" );
               }
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Export
   /////////////////////////////////////////////////////////////////////////////

   public CompletableFuture<Void> startExport ( File target ) {
      gui.stateRunning();
      gui.setProgress( -1.0 );
      state.done = 0;
      log.log( Level.CONFIG, "Export target: {0}", target );
      return runTask( () -> {
         // Export process is mainly IO limited. Not wasting time to make it multi-thread.
         String root = target.getPath().replaceFirst( "\\.html?$", "_files/" );
         log.log( Level.CONFIG, "Export root: {0}", target );
         new File( root ).mkdirs();

         checkStop( "Writing main catlog" );
         exporter.writeCatalog( root, categories );

         checkStop( "Loading content" );
         dal.loadEntityContent( categories, state );
         convertDataForExport();

         checkStop( "Writing data" );
         state.done = 0;
         state.update();
         for ( Category category : categories ) {
            log.log( Level.FINE, "Writing {0}", category.id );
            exporter.writeCategory( root, category, state );
         }

         checkStop( "Writing viewer" );
         exporter.writeViewer( target );

         gui.stateCanExport( "Export complete, may view data" );
      } ).whenComplete( terminate( "Export", gui::stateCanExport ) );
   }

   private void convertDataForExport () {
      checkStop( "Formatting and Indexing" );
      state.done = 0;
      state.update();
      for ( Category category : categories ) {
         log.log( Level.FINE, "Indexing {0}", category.id );
         Convertor convertor = Convertor.getConvertor( category, gui.isDebugging() );
         convertor.convert( state );
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Shared / Utils
   /////////////////////////////////////////////////////////////////////////////

   private CompletableFuture<Void> runTask ( RunExcept task ) {
      final CompletableFuture<Void> result = new CompletableFuture<>();
      threadPool.execute( ()-> { try {
         synchronized ( this ) { currentThread = Thread.currentThread(); }
         synchronized ( categories ) {
            task.run();
         }
         result.complete( null );

      } catch ( Exception e ) {
         result.completeExceptionally( e );
      } } );
      return result;
   }

   private void runAndCheckLogin ( String taskName, RunExcept task ) throws Exception {
      runAndGet( taskName, task );
      if ( crawler.needLogin() ) {
         log.log( Level.INFO, "Requires login: {0}", engine.getLocation() );
         runAndGet( "Open login page", crawler::openLoginPage );
         runAndGet( "Login", () -> crawler.login( gui.getUsername(), gui.getPassword() ) );
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
    * Call a task and wait for browser to finish loading.
    * The task must cause the browser's loader to change state for this to work.
    *
    * @param taskName Name of task, used in logging and timeout message.
    * @param task Task to run.
    */
   private void runAndGet ( String taskName, RunExcept task ) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      Consumer handle = browserTaskHandler( future, taskName );
      try {
         if ( INTERVAL_MS > 0 ) {
            lastLoad = lastLoad.plusMillis( INTERVAL_MS );
            long sleep = Duration.between( lastLoad, Instant.now() ).toMillis();
            if ( sleep < INTERVAL_MS ) Thread.sleep( INTERVAL_MS - sleep );
         }
         lastLoad = Instant.now();
         browser.getConsoleOutput().clear();
         browser.handle(
            ( e ) -> handle.accept( null ), // on load
            ( e,err ) -> handle.accept( err ) // on error
         );
         checkStop( taskName );
         task.run();
         synchronized ( future ) { future.wait(); }
         future.get(); // ExecutionException
      } catch ( Exception e ) {
         handle.accept( e );
         throw new RuntimeException( e );
      }
   }

   /**
    * Given a CompletableFuture,
    * set a timeout and return a Consumer function
    * that, when called, will finish the task
    * normally or exceptionally and cleanup.
    *
    * Clean up includes stopping the timeout, clear browser handlers,
    * and notify all threads waiting on the task.
    * If the task ended in exception, it will also be logged at warning level.
    *
    * @param task Task to timeout and complete
    * @param timeout_message Message of timeout exception
    * @return A function to call to complete
    */
   private Consumer browserTaskHandler ( CompletableFuture task, String taskName ) {
      BiConsumer<TimerTask, Object> handler = ( timeout, result ) -> { synchronized ( task ) {
         if ( timeout != null )
            timeout.cancel();
         browser.handle( null, null );
         if ( result != null && result instanceof Throwable ) {
            log.log( Level.WARNING, "Task finished exceptionally: {0}", result );
            task.completeExceptionally( (Throwable) result );
         } else {
            log.log( Level.FINE, "{0} finished normally.", taskName );
            task.complete( result );
         }
         task.notify();
      } };
      TimerTask openTimeout = Utils.toTimer( () -> handler.accept( null, new TimeoutException( taskName + " timeout" ) ) );
      scheduler.schedule( openTimeout, TIMEOUT_MS );
      return ( input ) -> handler.accept( openTimeout, input );
   }

   private static interface RunExcept {
      void run ( ) throws Exception;
   }
}