package db4e;

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
public class Downloader {

   private static final Logger log = Main.log;

   static final int DEF_TIMEOUT_MS = 30_000;
   static final int DEF_INTERVAL_MS = 1_000;
   static final int MIN_TIMEOUT_MS = 10_000;
   static final int MIN_INTERVAL_MS = 0;

   static volatile int TIMEOUT_MS = 30_000;
   static volatile int INTERVAL_MS = 1_000;

   static final String DB_NAME = "dnd4_compendium.database";

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
   private final Timer scheduler = new Timer();
   private final ForkJoinPool threadPool = ForkJoinPool.commonPool(); // Only need one thread

   public Downloader ( SceneMain main ) {
      gui = main;
      browser = main.getWorker();
      engine = browser.getWebEngine();
      crawler = new Crawler( engine );
      state = new ProgressState( main::setProgress );
      try {
         TIMEOUT_MS = Integer.parseUnsignedInt( main.txtTimeout.getText() ) * 1000;
         INTERVAL_MS = Integer.parseUnsignedInt( main.txtInterval.getText() );
      } catch ( NumberFormatException ignored ) {}
   }

   /////////////////////////////////////////////////////////////////////////////
   // Stop task
   /////////////////////////////////////////////////////////////////////////////

   void stop () {
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

   // Return a function that is used to clean up a future and perhaps display error message
   private BiConsumer<Void,Throwable> terminate ( String action ) {
      return ( result, err ) -> {
         if ( err != null ) {
            Throwable ex = err;
            if ( ex instanceof CompletionException && ex.getCause() != null )
               ex = ex.getCause();
            while ( ( ex instanceof RuntimeException || ex instanceof ExecutionException ) && ex.getCause() != null )
               ex = ex.getCause(); // Unwrap RuntimeException and ExecutionExceptiom

            if ( ex instanceof InterruptedException )
               gui.stateCanDownload( action + " stopped" );
            else if ( ex instanceof TimeoutException )
               gui.stateCanDownload( action + " timeout" );
            else {
               log.log( Level.WARNING, action + " failed: {0}", Utils.stacktrace( err ) );
               String msg = ( (Exception) err ).getMessage();
               if ( msg.contains( "Exception: ") ) msg = msg.split( "Exception: ", 2 )[1];
               gui.stateCanDownload( msg );
               if ( ex instanceof LoginException )
                  gui.txtUser.requestFocus();
            }
         }
         state.update();
         currentThread = null;
         Thread.interrupted(); // Clear flag
      };
   }

   /////////////////////////////////////////////////////////////////////////////
   // Open, Close, and Reset
   /////////////////////////////////////////////////////////////////////////////

   void resetDb () {
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

   CompletableFuture<Void> open( TableView categoryTable ) {
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
               gui.btnClearData.setDisable( false );
               closeDb();
               throw new RuntimeException( ex );
            }
         }

         log.log( Level.CONFIG, "Opened database {0}", db_file );
         if ( categoryTable != null ) categoryTable.setItems( categories );
         openOrCreateTable();
      } );
   }

   void close() {
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

      if ( state.done < state.total )
         gui.stateCanDownload( "Ready to download" );
      else
         gui.stateCanExport( "Ready to export" );
      state.update();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Download
   /////////////////////////////////////////////////////////////////////////////

   // Open compendium
   CompletableFuture<Void> startDownload () {
      gui.stateRunning();
      gui.setProgress( -1.0 );
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );
      log.log( Level.CONFIG, "Timeout {0} ms / Interval {1} ms ", new Object[]{ TIMEOUT_MS, INTERVAL_MS } );
      final CompletableFuture<Void> task = new CompletableFuture<>();

      threadPool.execute( ()-> { try {
         synchronized ( this ) { currentThread = Thread.currentThread(); }
         runAndCheckLogin( "Connect compendium", crawler::randomGlossary );
         downloadCategory();

         gui.stateCanExport( "Download complete, may export data" );
         task.complete( null );

      } catch ( Exception e ) {
         task.completeExceptionally( e );
      } } );

      return task.whenComplete( terminate( "Download" ) );
   }

   private void downloadCategory() throws Exception { synchronized ( categories ) { // Too many exceptions to throw one by one
      TransformerFactory factory = null;

      for ( Category category : categories ) {
         if ( category.total_entry.get() > 0 ) continue;
         final String name = category.name.toLowerCase();

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
      downloadEntities();
   } }

   private void downloadEntities () throws Exception {
      for ( Category category : categories ) {
         for ( Entry entry : category.entries ) {
            if ( ! entry.contentDownloaded ) {
               String name = entry.name + " (" + category.name + ")";
               runAndCheckLogin( name, () -> crawler.openEntry( entry ) );
               crawler.getEntry( entry );
               dal.saveEntry( entry );
               category.downloaded_entry.set( category.downloaded_entry.get() + 1 );
               state.done += 1;
               state.update();
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Export
   /////////////////////////////////////////////////////////////////////////////

   CompletableFuture<Void> startExport ( File target ) {
      gui.stateRunning();
      log.log( Level.CONFIG, "Export target: {0}", target );
      final CompletableFuture<Void> task = new CompletableFuture<>();

      threadPool.execute( ()-> { try {
         synchronized ( this ) { currentThread = Thread.currentThread(); }
         gui.setStatus( "Loading content" );
         synchronized ( categories ) {
            dal.loadEntityContent( categories, state );
         }

         gui.stateCanExport( "Export Complete, may view data" );
         task.complete( null );

      } catch ( Exception e ) {
         task.completeExceptionally( e );
      } } );

      return task.whenComplete( terminate( "Export" ) );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private void runAndCheckLogin ( String taskName, RunExcept task ) throws Exception {
      runAndGet( taskName, task );
      if ( crawler.needLogin() ) {
         log.log( Level.INFO, "Requires login: {0}", engine.getLocation() );
         runAndGet( "Open login page", crawler::openLoginPage );
         final String user = gui.txtUser.getText().trim();
         final String pass = gui.txtPass.getText().trim();
         runAndGet( "Login", () -> crawler.login( user, pass ) );
         // Post login page may contain forms (e.g. locate a store), so rerun task before check
         runAndGet( taskName, task );
         if ( crawler.needLogin() ) {
            log.log( Level.INFO, "Login failed: {0}", engine.getLocation() );
            throw new LoginException( "Login incorrect or expired, see Help." );
         }
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
      BiConsumer<TimerTask, Object> result = ( timeout, err ) -> {
         synchronized ( task ) {
            if ( timeout != null )
               timeout.cancel();
            browser.handle( null, null );
            if ( err != null && err instanceof Throwable ) {
               log.log( Level.WARNING, "Task finished exceptionally: {0}", err );
               task.completeExceptionally( (Throwable) err );
            } else {
               log.log( Level.FINE, "{0} finished normally.", taskName );
               task.complete( err );
            }
            task.notify();
         }
      };
      TimerTask openTimeout = Utils.toTimer( () -> result.accept( null, new TimeoutException( taskName + " timeout" ) ) );
      scheduler.schedule( openTimeout, TIMEOUT_MS );
      return ( err ) -> result.accept( openTimeout, err );
   }

   private void waitFinish ( CompletableFuture task, Consumer<? super InterruptedException> interrupted ) {
      synchronized ( task ) { try {
         task.wait();
      } catch ( InterruptedException ex ) {
         if ( interrupted != null )
            interrupted.accept( ex );
      } }
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
         waitFinish( future, handle );
         future.get(); // ExecutionException
      } catch ( Exception e ) {
         handle.accept( e );
         throw new RuntimeException( e );
      }
   }

   private static interface RunExcept {
      void run ( ) throws Exception;
   }
}