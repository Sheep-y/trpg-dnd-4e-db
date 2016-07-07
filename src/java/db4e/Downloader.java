package db4e;

import db4e.data.Catalog;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.web.WebEngine;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import sheepy.util.Utils;

/**
 * Data Management
 */
public class Downloader {

   private static final Logger log = Main.log;

   private static final String DB_NAME = "dnd4_compendium.sqlite";

   // Database variables are set on open().
   // Access must be synchronised with 'this'
   private SqlJetDb db; 
   private DbAbstraction dal; 
      
   private final Catalog cat = new Catalog();

   private final SceneMain gui;
   private final WebEngine browser;

   public Downloader ( SceneMain main ) {
      gui = main;
      browser = main.getWorkerEngine();
   }
   
   CompletableFuture<Void> resetDb () {
      gui.enterBusy( "Clearing data" );

      return CompletableFuture.completedFuture( null ).thenComposeAsync( ( ignored ) -> { try {
         synchronized ( this ) { // Lock database for the whole duration
            close();
            Thread.sleep( 1000 ); // Give OS some time to close the handle
            final File file = new File( DB_NAME );
            if ( file.exists() ) {
               log.log( Level.INFO, "Deleting database {0}", new File( DB_NAME ).getAbsolutePath() );
               file.delete();
            } else
               log.log( Level.WARNING, "Database file not found: {0}", new File( DB_NAME ).getAbsolutePath() );
            return this.open();
         }

      } catch ( Exception ex ) {
         log.log( Level.WARNING, "Error when deleting database: {0}", Utils.stacktrace( ex ) );
         open().whenComplete( ( a, b ) -> gui.setStatus( "Cannot clear data" ) );
         throw new RuntimeException( ex );

      } } );
   }

   CompletableFuture<Void> open() {
      gui.enterBusy( "Opening Database" );

      return CompletableFuture.runAsync( () -> {
         try {
            log.log( Level.INFO, "Opening database {0}", DB_NAME );
            synchronized( this ) {
               db = SqlJetDb.open(new File( DB_NAME ), true );
               dal = new DbAbstraction();
            }
         } catch ( Exception ex ) {
            log.log( Level.SEVERE, "Cannot open database: {0}", Utils.stacktrace( ex ) );
            gui.enterBusy( "Cannot open database" );
            gui.btnClearData.setDisable( false );
            close();
            throw new RuntimeException( ex );
         }

         log.log( Level.FINE, "Database opened. Loading tables." );
         openOrCreateTable();
      } );
   }

   synchronized void close() {
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
         int version = dal.setDb( db );
         log.log( Level.CONFIG, "Database version {0}.  Tables opened.", version );

      } catch ( Exception e1 ) {

         log.log( Level.CONFIG, "Create tables because {0}", Utils.stacktrace( e1 ) );
         try {
            dal.createTables();

         } catch ( Exception e2 ) {
            log.log( Level.SEVERE, "Cannot create tables: {0}", Utils.stacktrace( e2 ) );
            gui.enterBusy( "Cannot open database, try clear data" );
            gui.btnClearData.setDisable( false );
            close();
            throw new RuntimeException( e2 );
         }
      }
      gui.enterIdle ( "Ready to go" );
   }

}