package db4e;

import db4e.lang.ENG;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sheepy.util.JavaFX;
import updater.Updater;

public class Downloader extends Application {

   // Main method.  Do virtually nothing.
   public static void main( String[] args ) { launch( args ); }

   // System utilities
   public ResourceBundle res;
   public static final Logger log = Logger.getLogger( Downloader.class.getName() );
   public static final Preferences prefs = Preferences.userNodeForPackage( Downloader.class );

   // App Data
   private File current = new File( prefs.get( "app_folder", "4e_database.html" ) );
   public final Browser worker = new Browser( this );
   public final Updater updater = new Updater( this );

   // Interactive components
   private WebView webGuide;
   private final Button btnFolder = new Button();
   private final Label  lblFolder = new Label();
   private final Label  lblStatus = new Label();
      private String statusTextKey = "data.status.no_folder";
   private final Button btnDelete = new Button();
   private final Button btnResave = new Button();
   private final Button btnUpdate = new Button();
   private final TextArea txtLog = new TextArea();

   // Layout regions
   private Stage stage;
   private final BorderPane pnlGuide = new BorderPane( webGuide );
   private final BorderPane pnlDataTop = new BorderPane( null, null, lblFolder, lblStatus, btnFolder );
   private final GridPane   pnlDataAction = new GridPane();
   private final BorderPane pnlDataCentre = new BorderPane( null, pnlDataAction, null, pnlDataTop, null );
   private final BorderPane pnlData = new BorderPane( pnlDataCentre, pnlDataTop, null, null, null );
   private final TabPane pnlC = new TabPane(
         new Tab( "?", pnlGuide ),
         new Tab( "!", pnlData ),
         new Tab( "🌐", worker.getPanel() ),
         new Tab( "…", txtLog ) );

/********************************************************************************************************
 * UI code
 ********************************************************************************************************///

   @Override public void start( Stage stage ) throws Exception {
      this.stage = stage;
      initLogger();
      initTabs();
      localise( Locale.getDefault().getISO3Language() );

      // Listen for resize or close and update or save preferences
      stage.widthProperty() .addListener( (prop,old,now) -> prefs.putInt( "frmMain.width" , now.intValue() ) );
      stage.heightProperty().addListener( (prop,old,now) -> prefs.putInt( "frmMain.height", now.intValue() ) );
      stage.setOnCloseRequest( e -> { try {
         updater.stop();
         prefs.flush();
         log.getHandlers()[0].close();
      } catch ( BackingStoreException | SecurityException | NullPointerException ignored ) {} } );

      // Launch!
      stage.setScene( new Scene( pnlC, prefs.getInt( "frmMain.width", 750 ), prefs.getInt( "frmMain.height", 550 ) ) );
      stage.show();
      log.info( "log.init" );

      // Load previous location
      if ( current.exists() ) {
         try {
            loadFile( current );
            return;
         } catch ( IllegalArgumentException ex ) {
            log.log( Level.WARNING, "log.data.err.not_compendium", current );
         } catch ( IOException ex ) {
            log.log( Level.WARNING, "log.cannot_read", ex );
         }
      }
      unloadFile();
   }

   public void initLogger() throws SecurityException {
      final List<LogRecord> cache = new ArrayList<>();

      // Disable global logger
      Logger.getLogger( "" ).getHandlers()[0].setLevel( Level.OFF );

      // Setup our logger which goes to log tab.
      log.setLevel( Level.FINE );
      log.addHandler( new Handler() {
         private boolean closed = false;
         @Override public void publish( LogRecord record ) {
            if ( closed ) return;
            synchronized ( cache ) {
               if ( cache.isEmpty() ) // If cache is empty, queue a update.
                  Platform.runLater( () -> {
                     LogRecord[] records;
                     synchronized ( cache ) { // Copy from cache and work with local copy
                        records = cache.toArray( new LogRecord[ cache.size() ] );
                        cache.clear();
                     }
                     StringBuilder str = new StringBuilder(); // Concat all logs so that UI is only updated once
                     for ( LogRecord log : records )
                        str.append( getFormatter().formatMessage( log ) ).append( '\n' );
                     txtLog.insertText( txtLog.getLength(), str.toString() );
                  } );
               cache.add( record );
            }
         }
         @Override public void flush() {}
         @Override public void close() throws SecurityException { closed = true; }
      } );
      log.getHandlers()[0].setFormatter( new SimpleFormatter() );
   }

   public void initTabs() {
      ObservableList<Tab> tabs = pnlC.getTabs();
      for ( Tab tab : tabs ) tab.setClosable( false );

      pnlDataAction.add( btnDelete, 0, 0 );
      pnlDataAction.add( btnResave, 1, 0 );
      pnlDataAction.add( btnUpdate, 2, 0 );

      tabs.get( 2 ).setDisable( true );

      pnlC.getSelectionModel().select( 1 );
      pnlC.getSelectionModel().selectedItemProperty().addListener( (prop,old,now) -> {
         if ( now.getContent() == pnlGuide && webGuide == null ) {
            webGuide = new WebView();
            pnlGuide.setCenter( webGuide );
            localise();
         }
      } );
   }

   // Localise user interface
   public void localise () {
      localise( null );
   }
   public void localise ( String lang ) {
      if ( lang != null ) {
         switch ( lang ) {
//            case "zho": res = new ZHO(); break;
            default   : res = new ENG();
         }
         log.setResourceBundle( res );
         log.log( Level.CONFIG, "log.l10n", res.getClass().getSimpleName() );
      }
      stage.setTitle( res.getString( "title" ) );
      pnlC.getTabs().get( 0 ).setText( res.getString( "guide.title" ) );
      pnlC.getTabs().get( 1 ).setText( res.getString( "data.title"  ) );
         btnFolder.setText( res.getString( "data.btn.location" ) );
         btnFolder.setOnAction( this::btnFolder_action );
         btnDelete.setText( res.getString( "data.btn.delete" ) );
         btnDelete.setTooltip( new Tooltip( res.getString( "data.btn.delete_hint" ) ) );
         btnDelete.setOnAction( this::btnDelete_action );
         btnResave.setText( res.getString( "data.btn.resave" ) );
         btnResave.setTooltip( new Tooltip( res.getString( "data.btn.resave_hint" ) ) );
         btnUpdate.setText( res.getString( "data.btn.update" ) );
         btnUpdate.setTooltip( new Tooltip( res.getString( "data.btn.update_hint" ) ) );
      pnlC.getTabs().get( 2 ).setText( res.getString( "web.title" ) );
      pnlC.getTabs().get( 3 ).setText( res.getString( "log.title" ) );

      lblStatus.setText( resFormat( statusTextKey ) );

      if ( dlgOpen != null )
         dlgOpen.setTitle( "data.dlg.location.title" );

      if ( confirmDelete != null ) {
         confirmDelete.setTitle( resFormat( "data.dlg.delete.title" ) );
         confirmDelete.setContentText( resFormat( "data.dlg.delete.text" ) );
      }

      if ( webGuide != null ) webGuide.getEngine().loadContent( res.getString( "guide.html" ) );
   }

   private void setStatus ( String key ) {
      statusTextKey = key;
      lblStatus.setText( resFormat( key ) );
   }
   private String resFormat ( String key, Object ... params ) {
      key = res.getString( key );
      if ( params.length <= 0 ) return key;
      return MessageFormat.format( key, params );
   }

/********************************************************************************************************
 * Data tab
 ********************************************************************************************************///

   private void loadFile ( File f ) throws IOException, IllegalArgumentException {
      String content = Utils.loadFile( f );
      if ( ! content.contains( "https://github.com/Sheep-y/trpg-dnd-4e-db/" ) )
         throw new IllegalArgumentException();
      current = f;
      File file = new File( f.getParent(), f.getName().replaceAll( "\\.x?html?$", "" ) + "_files" );
      disableButtons();
      lblFolder.setText( file.getAbsolutePath() );

      new Thread( () -> {
         updater.stop();
         updater.isRead.addListener( new ChangeListener<Boolean>() {
            @Override public void changed( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
               if ( ! newValue ) return;
               updater.isRead.removeListener( this );
               Platform.runLater( Downloader.this::enableButtons );
            }
         } );
         Platform.runLater( () -> {
            btnFolder.setDisable( false );
            setStatus( "data.status.reading" );
         } );
         updater.setBasePath( file );
      } ).start();
   }

   private void unloadFile() {
      disableButtons();
      new Thread( () -> {
         updater.stop();
         Platform.runLater( () -> {
            lblFolder.setText( "" );
            setStatus( "data.status.no_folder" );
            btnFolder.setDisable( false );
         } );
      } ).start();
   }

   private void disableButtons() {
      setStatus( "data.status.unloading" );
      btnFolder.setDisable( true );
      btnDelete.setDisable( true );
      btnResave.setDisable( true );
      btnUpdate.setDisable( true );
   }

   private void enableButtons() {
      setStatus( "data.status.ready" );
      btnDelete.setDisable( false );
      btnResave.setDisable( false );
      btnUpdate.setDisable( false );
   }

   private FileChooser dlgOpen;
   public void btnFolder_action ( ActionEvent evt ) {
      assert( Platform.isFxApplicationThread() );

      // Create file dialog
      if ( dlgOpen == null ) {
         dlgOpen = new FileChooser();
         dlgOpen.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter( resFormat( "data.dlg.location.filter.db" ), "4e_database.html" ),
            new FileChooser.ExtensionFilter( resFormat( "data.dlg.location.filter.any" ), "*.*" ) );
         dlgOpen.setInitialDirectory( current.getParentFile() );
         localise();
      }

      // Show dialog
      File selected = dlgOpen.showOpenDialog( stage );
      if ( selected != null && selected.exists() && selected.isFile() ) {
         String msg = null;
         try {
            loadFile( selected );
            prefs.put( "app_folder", selected.toString() );
         } catch ( IllegalArgumentException ex ) {
            msg = resFormat( "log.data.err.not_compendium", selected );
         } catch ( IOException ex ) {
            msg = resFormat( "log.cannot_read", ex );
         }
         if ( msg != null ) {
            new Alert( Alert.AlertType.ERROR, msg, ButtonType.OK ).show();
            unloadFile();
         }
      }
   }

   Alert confirmDelete;
   public void btnDelete_action ( ActionEvent evt ) {
      assert( Platform.isFxApplicationThread() );

      if ( confirmDelete == null ) {
         confirmDelete = JavaFX.dialogDefault( new Alert( Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO ), ButtonType.NO );
         localise();
      }
      if ( ! confirmDelete.showAndWait().filter( e -> e == ButtonType.YES ).isPresent() )
         return;

      updater.deleteCategory();
      btnDelete.setDisable( true );
   }

}