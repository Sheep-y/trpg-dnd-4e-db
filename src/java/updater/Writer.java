package updater;

import db4e.Downloader;
import db4e.data.Catalog;
import db4e.data.Category;
import db4e.data.Entry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to save data to local files
 */
public class Writer implements Runnable {

   public static final Logger log = Logger.getLogger( Downloader.class.getName() );

   private volatile boolean stop;
   private final AtomicBoolean stopped = new AtomicBoolean( false );
   private final Thread thread;
   private final File basepath;
   private final List<Object> queue = new ArrayList<>();

   public Writer ( File basepath ) {
      this.basepath = basepath;
      Thread thread = new Thread( this, "Writer" );
      this.thread = thread;
      thread.setDaemon( false );
//      thread.setPriority( Thread.NORM_PRIORITY-1 );
   }

   public void start() {
      thread.start();
   }

   @Override public void run() {
      while ( true ) {
         Object[] write_list;
         synchronized ( queue )  { // Copy queue to local variable
            write_list = queue.toArray( new Object[ queue.size() ] );
            queue.clear();
         }
         for ( Object o : write_list ) {
            if ( o instanceof Entry )
               writeEntry( ( Entry ) o );
            else if ( o instanceof Category )
               writeCategory( ( Category ) o );
            else if ( o instanceof Catalog )
               writeCatalog( ( Catalog ) o );
            else
               log.log( Level.WARNING, "Unknown class in write queue: {0}", o.getClass() );
         }
         synchronized ( queue ) {
            if ( queue.isEmpty() ) {
               if ( stopped() ) break;
               waitForInput();
            }
         }
      }
      stopped.set( true );
      synchronized ( stopped ) {
         stopped.notifyAll();
      }
   }

   private void writeEntry ( Entry entry ) {
   }

   private void writeCategory ( Category category ) {
   }

   private void writeCatalog ( Catalog catalog ) {
   }

   /*************************************************************************************************************///

   public void stop () {
      stop = true;
      recheck();
   }

   private boolean stopped () {
      return stop || thread.isInterrupted();
   }

   /**
    * Add an object to write queue.
    */
   public void write( Object object ) {
      assert( object != null );
      synchronized ( queue ) {
         queue.add( object );
         recheck();
      }
   }

   /**
    * Signal writer thread to check queue and exit condition
    */
   public void recheck ( ) {
      synchronized ( queue ) {
         queue.notify();
      }
   }

   /**
    * Blocks until write queue is cleared.
    */
   public void waitForDone () {
      assert( stopped() );
      assert( Thread.currentThread() != thread );
      try {
         recheck();
         synchronized ( stopped ) {
            if ( stopped.get() ) return;
            stopped.wait();
         }
      } catch (InterruptedException ex) {}
   }

   /**
    * Blocks until there are input.
    */
   private void waitForInput() {
      try {
         queue.wait();
      } catch (InterruptedException ex) { }
   }

}