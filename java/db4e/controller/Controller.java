package db4e.controller;

import db4e.Main;
import db4e.SceneMain;
import db4e.converter.Convert;
import db4e.converter.Converter;
import db4e.data.Category;
import db4e.data.Entry;
import db4e.exporter.Exporter;
import db4e.exporter.ExporterMain;
import db4e.exporter.ExporterRawCsv;
import db4e.exporter.ExporterRawHtml;
import db4e.exporter.ExporterRawJson;
import db4e.exporter.ExporterRawSql;
import db4e.exporter.ExporterRawTsv;
import db4e.exporter.ExporterRawXlsx;
import java.io.File;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
   public final ObservableList<Category> exportCategories = new ObservableArrayList<>();

   private final SceneMain gui;
   private ConsoleWebView browser;
   private WebEngine engine;
   private Crawler crawler;
   private final Timer scheduler = new Timer();
   private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( 2, 32, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

   public Controller ( SceneMain main ) {
      gui = main;
      state = new ProgressState( ( progress ) -> {
         checkStop( null );
         main.setProgress( progress );
      } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Stop task
   /////////////////////////////////////////////////////////////////////////////

   public void setThreadCount( int thread ) {
      if ( thread <= 0 )
         thread = Math.max( 2, Math.min( Runtime.getRuntime().availableProcessors(), 32 ) );
      else
         ++thread;
      threadPool.setCorePoolSize( thread );
      log.log( Level.CONFIG, "Thread count set to {0} plus one controll thread", thread - 1 );
   }

   public void stop () {
      synchronized ( this ) {
         if ( engine != null )
            engine.getLoadWorker().cancel();
         if ( currentThread != null )
            currentThread.interrupt();
      }
   }

   private void checkStop ( String status ) {
      if ( status != null ) gui.setStatus( status );
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
            while ( ( err.getClass() == RuntimeException.class || err instanceof ExecutionException ) && err.getCause() != null )
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
            } else if ( err instanceof OutOfMemoryError ) {
               // Don't allow further actions
               gui.stateBusy( "Failed: Out of Memory" );
               gui.setTitle( "Error" );
               // Try free up memory to stabilse program for log
               synchronized ( categories ) {
                  exportCategories.clear();
               }
               for ( Category category : categories ) synchronized ( category ) {
                  category.meta = null;
                  category.sorted = null;
                  category.index = null;
                  category.entries.clear();
               }
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
         exportCategories.clear();
      }
      Convert.reset();
      state.reset();
      state.total = 0;
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

   public void close () {
      stop();
      scheduler.cancel();
      threadPool.shutdown();
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
      gui.setStatus( "Checking data" );
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
         if ( state.get() <= 0 && ! hasReset && gui.getUsername().isEmpty() && gui.getPassword().isEmpty() )
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

   private synchronized void initCrawler () {
      if ( browser != null ) return;
      log.log( Level.INFO, "Initialise web crawler" );
      browser = gui.getWorker();
      engine = browser.getWebEngine();
      crawler = new Crawler( engine );
   }

   // Open compendium
   public CompletableFuture<Void> startDownload () {
      gui.setTitle( "Downloading" );
      gui.stateRunning();
      gui.setProgress( -1.0 );
      initCrawler();
      log.log( Level.CONFIG, "WebView Agent: {0}", engine.getUserAgent() );
      log.log( Level.CONFIG, "Timeout {0} ms / Interval {1} ms ", new Object[]{ TIMEOUT_MS, INTERVAL_MS } );
      return runTask( () -> {
         setPriority( Thread.NORM_PRIORITY );
         if ( Main.simulate.get() )
            log.info( "Login check skipped for simulation" );
         else if ( categories.stream().anyMatch( e -> e.total_entry.get() <= 0 ) )
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
         Document xsl = crawler.getCategoryXsl();

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
      int remainingCount = state.total - state.get(), second;
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

   public boolean hasOldExport ( String data_dir ) {
      return new File( data_dir + "Glossary/glossary1.js" ).exists() ||  // 3.5.1
             new File( data_dir + "Glossary/data99.js" ).exists(); // 3.5.2 and 3.5.3
   }

   public CompletableFuture<Void> deleteOldExport ( String data_dir ) {
      gui.setTitle( "Exporting" );
      gui.setStatus( "Deleting old export" );
      if ( new File( data_dir + "Glossary/glossary1.js" ).exists() )
         state.total = 25960; // Exact file count by ver 3.5.1. But just to show progress, no need to be perfect.
      else
         state.total = 1817; // File count ot ver 3.5.2 to 3.5.3.
      state.reset();
      return runTask( () -> {
         for ( File folder : new File( data_dir ).listFiles() )
            if ( folder.isDirectory() ) {
               for ( File file : folder.listFiles() ) {
                  file.delete();
                  state.addOne();
               }
               folder.delete(); // Folder case has changed in 3.5.2.
            }
         state.set( state.total );
      } );
   }

   public CompletableFuture<Void> startExport ( File target ) {
      gui.setTitle( "Exporting" );
      gui.setStatus( "Starting export" );
      gui.stateRunning();
      state.set( -1 );
      return runTask( () -> {
         setPriority( Thread.MIN_PRIORITY );
         checkStop( "Loading data" );
         dal.loadEntityContent( categories, state );

         checkStop( "Writing catlog" );
         try ( Exporter exporter = new ExporterMain() ) {
            exporter.setState( target, this::checkStop, state );
            Convert.beforeConvert( categories, exportCategories );
            exporter.preExport( exportCategories );
            checkStop( "Writing data" );
            exportEachCategory( exportCategories, exporter );
            exporter.postExport( exportCategories );
            Convert.afterConvert();
         }

         gui.stateCanExport( "Export complete, may view data" );
      } ).whenComplete( terminate( "Export", gui::stateCanExport ) );
   }

   public void startExportRaw ( File target ) {
      Exporter exporter;
      if ( target.getName().toLowerCase().endsWith( ".html" ) || target.getName().toLowerCase().endsWith( ".htm" ) )
         exporter = new ExporterRawHtml();
      else if ( target.getName().toLowerCase().endsWith( ".csv" ) )
         exporter = new ExporterRawCsv();
      else if ( target.getName().toLowerCase().endsWith( ".tsv" ) )
         exporter = new ExporterRawTsv();
      else if ( target.getName().toLowerCase().endsWith( ".json" ) )
         exporter = new ExporterRawJson();
      else if ( target.getName().toLowerCase().endsWith( ".sql" ) )
         exporter = new ExporterRawSql();
      else if ( target.getName().toLowerCase().endsWith( ".xlsx" ) )
         exporter = new ExporterRawXlsx();
      else {
         new Alert( Alert.AlertType.ERROR, "Unknown file type. Must be html, csv, tsv, json, or xlsx.", ButtonType.OK ).showAndWait();
         return;
      }
      exporter.setState( target, this::checkStop, state );
      gui.setTitle( "Dumping" );
      gui.setStatus( "Starting dump" );
      gui.stateRunning();
      state.set( -1 );
      runTask( () -> {
         setPriority( Thread.MIN_PRIORITY );
         checkStop( "Loading data" );
         dal.loadEntityContent( categories, state );

         try ( Exporter exp = exporter ) {
            checkStop( "Writing catlog" );
            exp.preExport( categories );
            checkStop( "Writing data" );
            exportEachCategory( categories, exp );
            exp.postExport( categories );
         }

         gui.stateCanExport( "Raw data dumped" );
      } ).whenComplete( terminate( "Dump", gui::stateCanExport ) );
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
      if ( Main.simulate.get() ) return;
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

   private void exportEachCategory ( List<Category> categories, Exporter exporter ) throws Exception {
      state.reset();
      state.update();
      log.log( Level.CONFIG, "Running category task in {0} threads: 1 control and {1} worker(s).", new Object[]{ threadPool.getCorePoolSize(), threadPool.getCorePoolSize()-1 } );
      try {
         Converter.stop.set( false );
         Exporter.stop.set( false );
         List<CompletableFuture<Void>> tasks = new ArrayList<>( categories.size() );
         for ( Category category : categories ) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add( future );
            threadPool.execute( () -> { try {
               log.log( Level.INFO, "Exporting category {0} in thread {1}.", new Object[]{ category.name, Thread.currentThread().getName() } );
               synchronized ( exporter ) { /* sync with exporter.setState */ }
               synchronized ( category ) {
                  exporter.export( category );
               }
               future.complete( null );
            } catch ( Throwable e ) {
               future.completeExceptionally( e );
            } } );
         }
         CompletableFuture.allOf( tasks.toArray( new CompletableFuture[ tasks.size() ] ) ).get();
      } catch ( Exception e ) {
         Converter.stop.set( true );
         Exporter.stop.set( true );
         throw e;
      }
   }

   /**
    * Same as Runnable, but throws Exception.
    */

   public static interface RunExcept {
      void run ( ) throws Exception;
   }
}