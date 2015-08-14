package updater;

import db4e.Main;
import db4e.data.Catalog;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Updater {

   public static final Logger log = Logger.getLogger( Main.class.getName() );

   // Internal status
   private enum STATE { UNLOADED, LOADED, READING, WRITING, UPDATING };
   private STATE state = STATE.UNLOADED;
   private BooleanProperty done = new SimpleBooleanProperty();
   // Public statuc hooks
   public ReadOnlyBooleanProperty isDone = ReadOnlyBooleanProperty.readOnlyBooleanProperty( done );

   private File basepath;
   private final Main main;
   public final Catalog data = new Catalog();
   private Reader reader;
   private Loader loader;
   private Writer writer;

   public Updater ( Main main ) {
      this.main = main;
      done.set( true );
   }

   public synchronized void stop () {
      if ( reader == null && writer == null ) return;
      log.fine( "log.updater.stopping" );
      if ( reader != null ) {
         reader.stop();
         reader = null;
      }
      if ( writer != null ) {
         writer.stop();
         writer.waitForDone();
         writer = null;
      }
      log.fine( "log.updater.stopped" );
   }

   public synchronized CompletionStage<Catalog> setBasePath ( File basepath ) {
      stop();
      data.clear();
      log.log( Level.CONFIG, "log.updater.rebase", basepath );

      done.set( false );
      CompletableFuture<Catalog> promise = new CompletableFuture<>();
      state = STATE.READING;
      reader = Reader.load( data, basepath );

      reader.isRunning.addListener( (prop,oldVal,running) -> { synchronized ( this ) {
         if ( running || reader == null ) return; // Do nothing if not done or already handled.
         if ( ! reader.isInterrupted() ) {
            writer = new Writer( basepath );
            writer.start();
            data.setWriter( writer );
            loader = new Loader();
            loader.start();
            state = STATE.LOADED;
         } else {
            data.clear();
            state = STATE.UNLOADED;
         }
         reader = null;
         promise.complete( data );
         done.set( true );
      } } );
      reader.start();
      return promise;
   }

   /******************************************************************************************************************/
   // Delete

   public synchronized void deleteCategory () {
      assert( writer != null );

      for ( Category cat : data.categories ) {
         for ( Entry e : cat.entries ) {
            writer.delete( e );
         }
         writer.delete( cat );
      }
      writer.delete( data );
   }
}