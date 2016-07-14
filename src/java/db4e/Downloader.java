package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
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
   private static final String DB_NAME = "dnd4_compendium.sqlite";

   // Database variables are set on open().
   // Access must be synchronised with 'this'
   private SqlJetDb db;
   private DbAbstraction dal;
   private Thread currentThread;
   private final AtomicBoolean paused = new AtomicBoolean();

   public final ObservableList<Category> categories = new ObservableArrayList<>();

   private final SceneMain gui;
   private final ConsoleWebView browser;
   private final Crawler crawler;
   private final Timer scheduler = new Timer();
   private final ForkJoinPool threadPool = ForkJoinPool.commonPool();
//      new ForkJoinPool( Math.max( 3, Runtime.getRuntime().availableProcessors() - 1 ) );

   public Downloader ( SceneMain main ) {
      gui = main;
      browser = main.getWorker();
      crawler = new Crawler( browser.getWebEngine() );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Pause and resume
   /////////////////////////////////////////////////////////////////////////////

   void pause () {
      synchronized ( paused ) {
         paused.set( true );
      }
   }

   void resume () {
      synchronized ( paused ) {
         paused.set( false );
         paused.notifyAll();
      }
   }

   synchronized void stop () {
      browser.getWebEngine().getLoadWorker().cancel();
      if ( currentThread != null )
         currentThread.interrupt();
      currentThread = null;
      resume();
   }

   private void checkPause () {
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
      gui.setStatus( "Opening online compendium" );

      CompletableFuture<Void> dbOpen = new CompletableFuture<>();
      Consumer<Throwable> handle = browserTaskHandler( dbOpen, "Online compendium timeout" );

      threadPool.execute( ()-> {
         checkPause();
         browser.handle(
            ( e ) -> handle.accept( null ), // on load
            ( e,err ) -> handle.accept( err ) // on error
         );
         crawler.openFrontpage();
         waitFinish( dbOpen, handle );
      } );

      return dbOpen.thenCompose( ( result ) -> {
         log.info( "Compendium opened." );
         return downloadCategory();

      } ).exceptionally( ( err ) -> {
         gui.stateCanDownload();
         if ( err instanceof Exception ) {
            if ( err.getCause() != null ) {
               if ( err.getCause() instanceof InterruptedException )
                  gui.setStatus( "Download Stopped" );
               else if ( err.getCause() instanceof TimeoutException )
                  gui.setStatus( "Download Timeout" );
            } else
               gui.setStatus( ( (Exception) err ).getMessage() );
         } else {
            gui.setStatus( err.getClass().getSimpleName() );
         }
         return null;
      });
   }

   private CompletableFuture<Void> downloadCategory() {
      gui.setStatus( "Loading categories" );
      CompletableFuture<Void> catLoad = new CompletableFuture<>();

      threadPool.execute( () -> {
         checkPause();
         try {
            for ( Category cat : categories ) {
               if ( cat.total_entry.get() > 0 ) continue;
               checkPause();
               gui.setStatus( "Listing " + cat.name );
               List<Entry> entries = crawler.openCategory( cat, progress( "Listing " + cat.name ) );
               checkPause();
               gui.setStatus( "Saving " + cat.name );
               dal.saveEntryList( cat, entries, progress( "Saving " + cat.name ) );
               checkPause();
               Thread.sleep( INTERVAL_MS );
            }
            catLoad.complete( null );

         } catch ( Exception err ) {
            catLoad.completeExceptionally( err );
         }
      } );

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
   private Consumer<Throwable> browserTaskHandler ( CompletableFuture task, String timeout_message ) {
      BiConsumer<TimerTask, Throwable> result = ( timeout, err ) -> {
         synchronized ( task ) {
            if ( timeout != null )
               timeout.cancel();
            browser.handle( null, null );
            if ( err != null ) {
               log.log( Level.WARNING, "Task finished exceptionally: {0}", Utils.stacktrace( err ) );
               task.completeExceptionally( err );
            } else {
               log.fine( "Task finished normally." );
               task.complete( null );
            }
            task.notify();
         }
      };
      TimerTask openTimeout = Utils.toTimer( () -> result.accept( null, new TimeoutException( timeout_message ) ) );
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

   private BiConsumer<Integer, Integer> progress( String task ) {
      return ( current, total ) -> gui.setStatus( task + " (" + current + "/" + total + ")" );
   }

}