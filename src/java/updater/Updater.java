package updater;

import db4e.Downloader;
import db4e.data.Catalog;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Updater {

   public static final Logger log = Logger.getLogger( Downloader.class.getName() );

   private final Downloader main;
   private final Catalog data;

   private File basepath;
   private Reader reader;
   private Writer writer;

   public BooleanProperty isReady = new SimpleBooleanProperty();

   public Updater ( Downloader main ) {
      this.main = main;
      this.data = main.data;
      isReady.set( false );
   }

   public synchronized void stop () {
      log.fine( "log.updater.stopping" );
      if ( reader != null )
         reader.stop();
      if ( writer != null ) {
         writer.stop();
         writer.waitForDone();
      }
      log.fine( "log.updater.stopped" );
   }

   public synchronized void setBasePath ( File basepath ) {
      stop();
      data.clear();

      isReady.set( false );
      reader = Reader.load( data, basepath );
      writer = new Writer( basepath );
      data.setWriter( writer );

      reader.isRunning.addListener( (prop,old,now) -> {
         if ( ! now ) isReady.set( true );
      } );
      reader.start();
      writer.start();
      log.log( Level.CONFIG, "log.updater.rebase", basepath );
   }
}