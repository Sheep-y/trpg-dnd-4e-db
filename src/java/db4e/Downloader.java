package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.web.WebEngine;
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

   public static final int TIMEOUT_MS = 9000_000; // It take a very long time to list powers.
   public static volatile int INTERVAL_MS = 2_000;
   static final String DB_NAME = "dnd4_compendium.sqlite";

   // Database variables are set on open().
   // Access must be synchronised with 'this'
   private SqlJetDb db;
   private DbAbstraction dal;
   private Thread currentThread;
   private final AtomicBoolean paused = new AtomicBoolean();

   public final ObservableList<Category> categories = new ObservableArrayList<>();

   private final SceneMain gui;
   private final ConsoleWebView browser;
   private final WebEngine engine;
   private final Crawler crawler;
   private final Timer scheduler = new Timer();
   private final ForkJoinPool threadPool = ForkJoinPool.commonPool();
//      new ForkJoinPool( Math.max( 3, Runtime.getRuntime().availableProcessors() - 1 ) );

   public Downloader ( SceneMain main ) {
      gui = main;
      browser = main.getWorker();
      engine = browser.getWebEngine();
      crawler = new Crawler( engine );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Pause and resume
   /////////////////////////////////////////////////////////////////////////////

   void pause () {
      synchronized ( paused ) {
         paused.set( true );
      }
   }

   boolean isPausing () {
      return paused.get();
   }

   void resume () {
      synchronized ( paused ) {
         paused.set( false );
         paused.notifyAll();
      }
   }

   synchronized void stop () {
      engine.getLoadWorker().cancel();
      if ( currentThread != null )
         currentThread.interrupt();
      currentThread = null;
      resume();
   }

   private void checkStop ( String status ) {
      if ( status != null ) gui.setStatus( status );
      synchronized ( this ) {
         currentThread = Thread.currentThread();
      }
      synchronized ( paused ) {
         if ( Thread.interrupted() )
            throw new RuntimeException( new InterruptedException() );
         while ( paused.get() ) {
            gui.setStatus( "Paused" );
            try {
               paused.wait();
            } catch ( InterruptedException ex ) {
               throw new RuntimeException( ex );
            }
            gui.setStatus( "Resuming" );
         }
      }
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

         log.log( Level.FINE, "Database opened. Loading tables." );
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

   private synchronized void openOrCreateTable() {
      try {
         int version = dal.setDb( db, categories );
         log.log( Level.CONFIG, "Database version {0,number,#}.  Tables opened.", version );

      } catch ( Exception e1 ) {

         log.log( Level.CONFIG, "Create tables because {0}", Utils.stacktrace( e1 ) );
         try {
            dal.createTables();
            int version = dal.setDb( db, categories );
            log.log( Level.FINE, "Created and opened tables.  Database version {0,number,#}.", version );

         } catch ( Exception e2 ) {
            log.log( Level.SEVERE, "Cannot create tables: {0}", Utils.stacktrace( e2 ) );
            gui.stateBadData();
            closeDb();
            throw new RuntimeException( e2 );
         }
      }
      gui.stateCanDownload();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Download
   /////////////////////////////////////////////////////////////////////////////

   // Open compendium
   CompletableFuture<Void> startDownload () {
      gui.stateRunning();
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );

      CompletableFuture<Void> dbOpen = new CompletableFuture<>();
      threadPool.execute( ()-> runAndGet( dbOpen, "Open compendium", crawler::openFrontpage ) );

      return dbOpen.thenCompose( ( result ) -> {
         log.info( "Compendium opened." );
         return downloadCategory();

      } ).thenRun( () -> {
        gui.stateCanDownload();
        gui.setStatus( "Download Complete, may export data" );

      } ).exceptionally( ( err ) -> {
         gui.stateCanDownload();
         if ( err.getCause() instanceof InterruptedException )
            gui.setStatus( "Download Stopped" );
         else if ( err.getCause() instanceof TimeoutException )
            gui.setStatus( "Download Timeout" );
         else {
            log.log( Level.WARNING, "Download failed: {0}", Utils.stacktrace( err ) );
            String msg = ( (Exception) err ).getMessage();
            if ( msg.contains( "Exception: ") ) msg = msg.split( "Exception: ", 2 )[1];
            gui.setStatus( msg );
         }
         return null;
      });
   }

   private CompletableFuture<Void> downloadCategory() {
      gui.setStatus( "Downloading categories" );
      CompletableFuture<Void> catLoad = new CompletableFuture<>();

      threadPool.execute(() -> { try {
         TransformerFactory factory = null;

         for ( Category cat : categories ) {
            if ( cat.total_entry.get() > 0 ) continue;
            final String name = cat.name.toLowerCase();

            Thread.sleep( INTERVAL_MS );
            CompletableFuture<Void> downXsl = runAndGet(null, "Get " + name + " template",
               () -> crawler.getCategoryXsl( cat ) );
            downXsl.get(); // throw ExecutionException if error
            Document xsl = engine.getDocument();

            Thread.sleep( INTERVAL_MS );
            CompletableFuture<Void> downXml = runAndGet(null, "Get " + name + " data",
               () -> crawler.getCategoryData( cat ) );
            downXml.get(); // throw ExecutionException if error
            Document xml = engine.getDocument();

            checkStop("Parsing " + name );
            if ( factory == null ) factory = TransformerFactory.newInstance();
            StringWriter result = new StringWriter();
            factory.newTransformer( new DOMSource( xsl ) ).transform( new DOMSource( xml ), new StreamResult( result ) );

            runAndGet(null, "Listing " + name,
               () -> Platform.runLater( () -> engine.loadContent( result.toString() ) ) );
            List<Entry> entries = crawler.openCategory();

            checkStop("Saving " + name );
            dal.saveEntryList( cat, entries );

            checkStop("Listed " + name );
         }
         catLoad.complete( null );

      } catch ( Exception err ) {
         if ( err instanceof ExecutionException )
            catLoad.completeExceptionally( err.getCause() );
         else
            catLoad.completeExceptionally( err );
      } } );

      return catLoad;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

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
   private Consumer browserTaskHandler ( CompletableFuture task, String task_name ) {
      BiConsumer<TimerTask, Object> result = ( timeout, err ) -> {
         synchronized ( task ) {
            if ( timeout != null )
               timeout.cancel();
            browser.handle( null, null );
            if ( err != null && err instanceof Throwable ) {
               log.log( Level.WARNING, "Task finished exceptionally: {0}", Utils.stacktrace( (Throwable) err ) );
               task.completeExceptionally( (Throwable) err );
            } else {
               log.log( Level.FINE, "{0} finished normally.", task_name );
               task.complete( err );
            }
            task.notify();
         }
      };
      TimerTask openTimeout = Utils.toTimer( () -> result.accept( null, new TimeoutException( task_name + " timeout" ) ) );
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

   private <T> CompletableFuture<T> runAndGet ( CompletableFuture<T> future, String task_name, RunExcept task ) {
      if ( future == null ) future = new CompletableFuture<>();
      Consumer handle = browserTaskHandler( future, task_name );
      try {
         browser.handle(
            ( e ) -> handle.accept( null ), // on load
            ( e,err ) -> handle.accept( err ) // on error
         );
         checkStop( task_name );
         task.run();
         waitFinish( future, handle );
      } catch ( Exception e ) {
         handle.accept( e );
      }
      return future;
   }

   private static interface RunExcept {
      void run ( ) throws Exception;
   }

   private BiConsumer<Integer, Integer> progress( String task ) {
      return ( current, total ) -> {
         checkStop( task + " (" + current + "/" + total + ")" );
      };
   }

}