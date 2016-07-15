package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.io.StringWriter;
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
import java.util.function.Function;
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
 * Data Management
 */
public class Downloader {

   private static final Logger log = Main.log;

   public static final int TIMEOUT_MS = 30_000;
   public static volatile int INTERVAL_MS = 2_000;
   static final String DB_NAME = "dnd4_compendium.sqlite";

   // Database variables are set on open().
   // Access must be synchronised with 'this'
   private SqlJetDb db;
   private DbAbstraction dal;
   private Thread currentThread;
   private boolean downloadComplete;

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
   }

   /////////////////////////////////////////////////////////////////////////////
   // Stop task
   /////////////////////////////////////////////////////////////////////////////

   synchronized void stop () {
      engine.getLoadWorker().cancel();
      if ( currentThread != null )
         currentThread.interrupt();
      currentThread = null;
   }

   private void checkStop ( String status ) {
      if ( status != null ) gui.setStatus( status );
      synchronized ( this ) {
         currentThread = Thread.currentThread();
      }
      if ( Thread.interrupted() )
         throw new RuntimeException( new InterruptedException() );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Open, Close, and Reset
   /////////////////////////////////////////////////////////////////////////////

   void resetDb () {
      gui.setStatus( "Clearing data" );
      categories.clear();

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
      gui.stateBusy( "Opening Database" );
      return CompletableFuture.runAsync( () -> {
         File db_file = new File( DB_NAME );
         String db_path = db_file.getAbsolutePath();
         try {
            log.log( Level.INFO, "Opening shared database {0}", db_path );
            synchronized( this ) {
               db = SqlJetDb.open( db_file, true );
               dal = new DbAbstraction();
            }
         } catch ( Exception err ) {
            log.log(Level.SEVERE, "Cannot open shared database {0}: {1}", new Object[]{ db_path, Utils.stacktrace( err ) } );

            db_file = new File( System.getProperty("user.home") + "/" + DB_NAME );
            db_path = db_file.getAbsolutePath();
            try {
               log.log( Level.INFO, "Opening user database {0}", db_path );
               synchronized( this ) {
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

         log.log( Level.FINE, "Database opened. Loading data." );
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

   private synchronized void openOrCreateTable () {
      try {
         downloadComplete = dal.setDb( db, categories, ( txt ) -> checkStop( "Reading data (" + txt + ")" ) );

      } catch ( Exception e1 ) {

         log.log( Level.CONFIG, "Create tables because {0}", Utils.stacktrace( e1 ) );
         try {
            dal.createTables();
            downloadComplete = dal.setDb( db, categories, ( txt ) -> checkStop( "Reading data (" + txt + ")" ) );

         } catch ( Exception e2 ) {
            log.log( Level.SEVERE, "Cannot create tables: {0}", Utils.stacktrace( e2 ) );
            gui.stateBadData();
            closeDb();
            throw new RuntimeException( e2 );
         }
      }
      if ( ! downloadComplete )
         gui.stateCanDownload();
      else
         gui.stateCanExport();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Download
   /////////////////////////////////////////////////////////////////////////////

   // Open compendium
   CompletableFuture<Void> startDownload () {
      gui.stateRunning();
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );
      final CompletableFuture<Void> task = new CompletableFuture<>();

      threadPool.execute( ()-> { try {
         runAndCheckLogin( "Connect compendium", crawler::randomGlossary );
         runAndGet( "Open compendium", crawler::openFrontpage );
         downloadCategory();

         gui.stateCanExport();
         gui.setStatus( "Download complete, may export data" );
         task.complete( null );

      } catch ( Exception e ) {
         task.completeExceptionally( e );
      } } );

      return task.exceptionally( terminate( "Download" ) );
   }

   private void downloadCategory() throws Exception { synchronized ( Category.class ) { // Too many exceptions to throw one by one
      TransformerFactory factory = null;

      for ( Category category : categories ) {
         if ( category.total_entry.get() > 0 ) continue;
         final String name = category.name.toLowerCase();

         Thread.sleep( INTERVAL_MS );
         runAndGet( "Get " + name + " template", () ->
            crawler.getCategoryXsl( category ) );
         Document xsl = engine.getDocument();

         Thread.sleep( INTERVAL_MS );
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
               category.downloaded_entry.add( 1 );
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
         dal.loadEntityContent( categories, ( txt ) -> checkStop( "Loading entry (" + txt + ")" ) );

         gui.stateCanExport();
         gui.setStatus( "Export Complete, may view data" );
         task.complete( null );

      } catch ( Exception e ) {
         task.completeExceptionally( e );
      } } );

      return task.exceptionally( terminate( "Export" ) );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private void runAndCheckLogin ( String taskName, RunExcept task ) throws Exception {
      runAndGet( taskName, task );
      if ( ! crawler.isLoginOk() ) {
         log.log( Level.INFO, "Requires login: {0}", engine.getLocation() );
         final String user = gui.txtUser.getText().trim();
         final String pass = gui.txtPass.getText().trim();
         runAndGet( taskName + " (Login)", () -> crawler.login( user, pass ) );
         if ( ! crawler.isLoginOk() ) {
            log.log( Level.INFO, "Login failed: {0}", engine.getLocation() );
            throw new LoginException( "Login incorrect or subscription expired" );
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
               log.log( Level.WARNING, "Task finished exceptionally: {0}", Utils.stacktrace( (Throwable) err ) );
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

   private Function<Throwable,Void> terminate ( String action ) {
       return ( err ) -> {
         synchronized ( this ) { currentThread = null; }
         Throwable ex = err;
         if ( ex instanceof CompletionException && ex.getCause() != null ) ex = ex.getCause();
         while ( ( ex.getClass() == RuntimeException.class || ex.getClass() == ExecutionException.class ) && ex.getCause() != null )
            ex = ex.getCause(); // Unwrap RuntimeException and ExecutionException (but not subclasses)

         gui.stateCanDownload();
         if ( ex.getCause() instanceof InterruptedException )
            gui.setStatus( action + " stopped" );
         else if ( ex.getCause() instanceof TimeoutException )
            gui.setStatus( action + " timeout" );
         else {
            log.log( Level.WARNING, action + " failed: {0}", Utils.stacktrace( err ) );
            String msg = ( (Exception) err ).getMessage();
            if ( msg.contains( "Exception: ") ) msg = msg.split( "Exception: ", 2 )[1];
            gui.setStatus( msg );
         }
         return null;
      };
   }

   private static interface RunExcept {
      void run ( ) throws Exception;
   }
}