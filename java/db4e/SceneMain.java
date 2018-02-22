package db4e;

import db4e.controller.Controller;
import static db4e.controller.Controller.DEF_INTERVAL_MS;
import static db4e.controller.Controller.DEF_RETRY_COUNT;
import static db4e.controller.Controller.DEF_TIMEOUT_MS;
import static db4e.controller.Controller.MIN_INTERVAL_MS;
import static db4e.controller.Controller.MIN_TIMEOUT_MS;
import db4e.data.Category;
import db4e.exporter.ExporterMain;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sheepy.util.JavaFX;
import sheepy.util.Resource;
import sheepy.util.Utils;
import sheepy.util.ui.ConsoleWebView;

/**
 * Main GUI of downloader.
 */
public class SceneMain extends Scene {

   private static final Logger log = Main.log;
   private static final Preferences prefs = Main.prefs;

   // Help Screen
   private final BorderPane pnlHelpTab = new BorderPane();
   private final Tab tabHelp = new Tab( "Help", pnlHelpTab );

   // Data Screen
   private final Label lblStatus = new Label( "Starting Up" );
   private final ProgressIndicator prgProgress = new ProgressIndicator( -1f );
   final TextField txtUser  = JavaFX.tooltip( new TextField( prefs.get( "ddi.user", "" ) ),
           "DDI subscriber username" );
   final PasswordField txtPass  = JavaFX.tooltip( new PasswordField(),
           "DDI subscriber password" );
   private final TableView<Category> tblCategory = new TableView<>();
      private final TableColumn<Category,String > colName = new TableColumn<>( "Category" );
      private final TableColumn<Category,Integer> colTotalEntry = new TableColumn<>( "Total" );
      private final TableColumn<Category,Integer> colDownloadedEntry = new TableColumn<>( "Downloaded" );
   private final Button btnLeft = new Button( "Wait" );
   private final Button btnRight = new Button( "Exit" );

   private final Pane pnlDataTab = new BorderPane( tblCategory,
      JavaFX.fitVBox(
         new HBox( JavaFX.maxSize( lblStatus ), prgProgress ),
         JavaFX.fitHBox( txtUser, txtPass ) ),  // Top
      null, JavaFX.fitHBox( btnLeft, btnRight ), null ); // right, bottom, left
   private final Tab tabData = new Tab( "Data", pnlDataTab );

   // Option Screen
   final TextField txtTimeout  = JavaFX.tooltip( new TextField( Integer.toString( Math.max( MIN_TIMEOUT_MS / 1000, prefs.getInt( "download.timeout", DEF_TIMEOUT_MS / 1000 ) ) ) ),
           "Download timeout in seconds." );
   final TextField txtInterval  = JavaFX.tooltip( new TextField( Integer.toString( Math.max( MIN_INTERVAL_MS, prefs.getInt( "download.interval", DEF_INTERVAL_MS ) ) ) ),
           "Minimal interval, in millisecond, between each download action." );
   final TextField txtRetry  = JavaFX.tooltip( new TextField( Integer.toString( Math.max( 0, prefs.getInt( "download.retry", DEF_RETRY_COUNT ) ) ) ),
           "Number of timeout retry.  Only apply to timeout errors." );
   final TextField txtAgent  = JavaFX.tooltip( new TextField( prefs.get( "download.agent", "" ) ),
           "Browser user agent to use to download data.  Leave empty for default." );
   final TextField txtThread  = JavaFX.tooltip( new TextField( Integer.toString( Math.max( 0, prefs.getInt( "export.thread", 0 ) ) ) ),
           "More thread exports faster but use more memory.  0 = Auto" );
   private final CheckBox chkFixAndEnhance = JavaFX.tooltip( new CheckBox( "Fix and enhance data" ),
           "Fix known issues in the data, make them consistent, add or enhance data properties, and ignore flavor text in full search." );
   private final CheckBox chkCompress = JavaFX.tooltip( new CheckBox( "Compress exported data" ),
           "Compress exported data files.  Actual speedup or slowdown depends on data speed, browser, and hardware." );
   private final CheckBox chkDebug = JavaFX.tooltip( new CheckBox( "Show debug tabs" ),
           "Show program log and console, and enable internal data assertions.  Will slow down download & export and use more memory." );
   final Button btnClearData = JavaFX.tooltip( new Button( "Clear Downloaded Data" ), // Allow downloader access, to allow clear when db is down
           "Clear ALL downloaded data by deleting '" + Controller.DB_NAME + "'." );
   final Button btnExportData = JavaFX.tooltip( new Button( "Dump Data" ),
           "Dump data in plain formats, such as Excel or CSV.  Uncheck \"Fix and enhance data\" above to export raw data." );
   final Button btnCheckUpdate = JavaFX.tooltip( new Button( "Check update" ),
           "Check for availability of new releases." );
   private final Pane pnlOptionTab = new VBox( 8,
           new HBox( 8, new Label( "Timeout in" ), txtTimeout, new Label( "seconds.") ),
           new HBox( 8, new Label( "Throttle" ), txtInterval, new Label( "milliseconds (minimal) per request.") ),
           new HBox( 8, new Label( "Retry" ), txtRetry, new Label( "times on timeout.") ),
           new HBox( 8, new Label( "Agent String" ), txtAgent ),
           new HBox( 8, new Label( "Export in" ), txtThread, new Label( "threads (0 = Auto)") ),
           chkFixAndEnhance,
           chkCompress,
           chkDebug,
           new HBox( 8, btnClearData, btnExportData ),
           btnCheckUpdate );
   private final Tab tabOption = new Tab( "Options", pnlOptionTab );

   // Log Screen
   private final TextArea txtLog = new TextArea();
   private final Tab tabLog = new Tab( "Log", new BorderPane( txtLog ) );

   // Worker Screen
   private ConsoleWebView pnlWorker;
   private final Tab tabWorker = new Tab( "Worker" );
   private final Controller loader = new Controller( this );

   // Layout regions
   private final TabPane pnlC = new TabPane( tabData, tabOption, tabHelp );

   public SceneMain( MainApp main ) {
      super( new Group(), 800, 500 );
      main.addLoggerOutput( txtLog );
      initControls();
      initLayout();
      initTabs();
      try {
         Controller.TIMEOUT_MS = Integer.parseUnsignedInt( txtTimeout.getText() ) * 1000;
         Controller.INTERVAL_MS = Integer.parseUnsignedInt( txtInterval.getText() );
         Controller.RETRY_COUNT = Integer.parseUnsignedInt( txtRetry.getText() );
         loader.setUserAgent( txtAgent.getText() );
         loader.setThreadCount( Integer.parseUnsignedInt( txtThread.getText() ) );
      } catch ( NumberFormatException ignored ) {}
      setRoot( pnlC );
   }

   private void initControls () {
      // Data tab - save preference on change
      prgProgress.addEventFilter( MouseEvent.MOUSE_CLICKED, ( evt ) -> this.action_view( null ) );
      txtPass.setText( prefs.get( "ddi.pass", "" ) );
      txtUser.setPromptText( "DDI login username (not email)" );
      txtPass.setPromptText( "DDI login password" );
      txtUser.textProperty().addListener( (prop, old, now ) -> { prefs.put( "ddi.user", now ); });
      txtPass.textProperty().addListener( (prop, old, now ) -> { prefs.put( "ddi.pass" , now ); });

      colName.setCellValueFactory( new PropertyValueFactory( "name" ) );
      colTotalEntry.setCellValueFactory( new PropertyValueFactory<>( "totalEntry" ) );
      colDownloadedEntry.setCellValueFactory( new PropertyValueFactory<>( "downloadedEntry" ) );
      tblCategory.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
      tblCategory.getColumns().addAll( colName, colDownloadedEntry, colTotalEntry );

      setRight( "Exit", this::action_exit );

      // Option tab
      txtTimeout.textProperty().addListener( (prop, old, now ) -> { try {
         int i = Integer.parseUnsignedInt( now );
         if ( i * 1000 < Controller.MIN_TIMEOUT_MS ) return;
         prefs.putInt( "download.timeout", i );
         Controller.TIMEOUT_MS = i * 1000;
         log.log( Level.CONFIG, "Timeout changed to {0} ms", Controller.TIMEOUT_MS );
      } catch ( NumberFormatException ignored ) { } } );
      txtTimeout.setPrefWidth( 80 );

      txtInterval.textProperty().addListener( (prop, old, now ) -> { try {
         int i = Integer.parseUnsignedInt( now );
         if ( i < Controller.MIN_INTERVAL_MS ) return;
         prefs.putInt( "download.interval", i );
         Controller.INTERVAL_MS = i;
         log.log( Level.CONFIG, "Interval changed to {0} ms", i );
      } catch ( NumberFormatException ignored ) { } } );
      txtInterval.setPrefWidth( 80 );

      txtRetry.textProperty().addListener( (prop, old, now ) -> { try {
         int i = Integer.parseUnsignedInt( now );
         if ( i < 0 ) return;
         prefs.putInt( "download.retry", i );
         Controller.RETRY_COUNT = i;
         log.log( Level.CONFIG, "Retry count changed to {0}", i );
      } catch ( NumberFormatException ignored ) { } } );
      txtRetry.setPrefWidth( 50 );

      txtAgent.textProperty().addListener( (prop, old, now ) -> {
         prefs.put( "download.agent", now );
         loader.setUserAgent( now );
      } );
      txtAgent.setPrefWidth( 600 );

      txtThread.textProperty().addListener( (prop, old, now ) -> { try {
         int i = Integer.parseUnsignedInt( now );
         if ( i < 0 ) return;
         prefs.putInt( "export.thread", i );
         loader.setThreadCount( i );
      } catch ( NumberFormatException ignored ) { } } );
      txtThread.setPrefWidth( 50 );

      setupCheckbox( chkFixAndEnhance, "export.fix", true, this::chkFix_change );
      setupCheckbox( chkDebug, "gui.debug", false, this::chkDebug_change );
      if ( loader.canCompressData() ) {
         setupCheckbox( chkCompress, "export.compress", false, this::chkCompress_change );
      } else {
         log.log( Level.CONFIG, "Data compression disabled because of lack of configurated java memory." );
         chkCompress.setText( chkCompress.getText() + " (Disabled; insufficient java memory)" );
         chkCompress.setDisable( true );
      }

      btnClearData.addEventHandler( ActionEvent.ACTION, this::btnClearData_click );
      btnExportData.addEventHandler( ActionEvent.ACTION, this::action_export_raw );
      btnCheckUpdate.addEventHandler( ActionEvent.ACTION, this::btnCheckUpdate_click );

      // Log tab
      txtLog.setEditable( false );

      stateBusy( "Initialising" );
   }

   private void setupCheckbox ( CheckBox checkbox, String preference, boolean defaultValue, ChangeListener<Boolean> listener ) {
      boolean state = prefs.getBoolean( preference, defaultValue );
      checkbox.selectedProperty().set( state );
      checkbox.selectedProperty().addListener( listener );
      listener.changed( checkbox.selectedProperty(), ! state, state );
   }

   private void initLayout () {
      Insets i8 = new Insets( 8 );

      pnlDataTab.setPadding( i8 );
         HBox.setHgrow( lblStatus, Priority.ALWAYS );
         lblStatus.setFont( new Font( lblStatus.getFont().getName(), 24 ) );
         lblStatus.setAlignment( Pos.CENTER );
         lblStatus.setPadding( i8 );
         btnLeft.setPadding( i8 );
         btnRight.setPadding( i8 );

      pnlOptionTab.setPadding( i8 );
   }

   private void initTabs () {
      ObservableList<Tab> tabs = pnlC.getTabs();
      for ( Tab tab : tabs )
         tab.setClosable( false );
      tabLog.setClosable( false );

      // Start at data tab
      pnlC.getSelectionModel().select( tabData );
      // Load help doc dynamically
      pnlC.getSelectionModel().selectedItemProperty().addListener( (prop,old,now) -> {
         if ( now == tabWorker ) {
            initWorkerTab();
            Platform.runLater( () -> pnlWorker.getConsoleInput().requestFocus() );
         } else if ( now == tabHelp )
            initWebViewTab( pnlHelpTab, "res/downloader_about.html" );
      } );
   }

   // Called by Main after stage show
   void startup() {
      loader.open( tblCategory ).thenRun( () -> Platform.runLater( () -> {
         setTitle( "ver. " + Main.VERSION );
         btnLeft.requestFocus();
         checkUpdate( false );
      } ) );
   }

   // Called by Main during stage shutdown
   void shutdown() {
      loader.close();
   }

   /////////////////////////////////////////////////////////////////////////////
   // GUI state
   /////////////////////////////////////////////////////////////////////////////

   public void setTitle ( String title ) { runFX( () -> {
      ( (Stage) getWindow() ).setTitle( Main.TITLE + ( title == null ? "" : " - " + title ) );
   } ); }

   public void selectTab ( String id ) { runFX( () -> {
      switch ( id ) {
         case "help":
            pnlC.getSelectionModel().select( tabHelp );
            break;

         default:
            log.log( Level.WARNING, "Unknown GUI tab id: {0}", id );
      }
   } ); }

   private void runFX ( Runnable r ) {
      if ( Platform.isFxApplicationThread() ) {
         r.run();
      } else {
         Platform.runLater( r );
      }
   }

   private void allowAction () {
      txtUser.setDisable( false );
      txtPass.setDisable( false );
      chkFixAndEnhance.setDisable( false );
      btnLeft.setDisable( false );
      btnClearData.setDisable( false );
      btnExportData.setDisable( false );
   }

   private void disallowAction () {
      txtUser.setDisable( true );
      txtPass.setDisable( true );
      chkFixAndEnhance.setDisable( true );
      btnLeft.setDisable( true );
      btnClearData.setDisable( true );
      btnExportData.setDisable( true );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Data tab
   /////////////////////////////////////////////////////////////////////////////

   public void setStatus ( String msg ) { runFX( () -> {
      log.log( Level.INFO, "Status: {0}.", msg );
      lblStatus.setText( msg );
   } ); }

   private final AtomicInteger lastProgress = new AtomicInteger();

   public void setProgress ( Double progress ) {
      int div = (int) ( progress * 100 );
      int last = lastProgress.getAndSet( div );
      if ( last != div ) runFX( () -> {
         log.log( Level.FINE, "Progress: {0}.", progress );
         prgProgress.setProgress( (double) div / 100 );
      } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Help & About
   /////////////////////////////////////////////////////////////////////////////

   private WebEngine popupHandler;

   /**
    * Create a webview, put it in the pane, and load its content async.
    * If the pane already has a Node at the center, this method will do nothing.
    *
    * @param pane Panel to add webview to
    * @return Created webview
    */
   private void initWebViewTab ( BorderPane pane, String doc ) {
      if ( pane.getCenter() != null ) return;

      log.log( Level.INFO, "Initialise help browser" );
      WebView web = new WebView();
      WebEngine engine = web.getEngine();
      pane.setCenter( web );
      engine.loadContent( "<h1>Loading</h1>" );
      engine.setCreatePopupHandler( ( popup ) -> {
         if ( popupHandler == null ) {
            assert( Platform.isFxApplicationThread() );
            popupHandler = new WebEngine();
            popupHandler.locationProperty().addListener( ( url, old, now ) -> { try {
               log.log( Level.FINE, "Call desktop to browse {0}", now );
               Desktop.getDesktop().browse( new URI( now ) );
               popupHandler.getLoadWorker().cancel();
            } catch ( Exception err ) {
               // Should not happen because invalid url won't trigger popup
               log.log( Level.WARNING, "Malformed URL: {0}", Utils.stacktrace( err ) );
               //new Alert( Alert.AlertType.ERROR, "Cannot open " + now, ButtonType.OK ).show();
            } } );
         }
         return popupHandler;
      } );
      new Thread( () -> {
         try {
            final String txt = Resource.getText( doc );
            runFX( () -> {
                  web.getEngine().loadContent( txt );
            } );
         } catch ( IOException err ) {
            log.log( Level.WARNING, "Error when loading help: {0}", Utils.stacktrace( err ) );
            runFX( () -> {
               web.getEngine().loadContent( "<h1>Cannot load help.</h1>"
                     + "<h2><a href='https://github.com/Sheep-y/trpg-dnd-4e-db'>Project Home</a.></h2>" );
            } );
         }
      } ).start();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Data Tab
   /////////////////////////////////////////////////////////////////////////////

   private void setLeft ( String text, EventHandler<ActionEvent> action ) {
      btnLeft.setText( text );
      btnLeft.onActionProperty().set( action );
   }

   private void setRight ( String text, EventHandler<ActionEvent> action ) {
      btnRight.setText( text );
      btnRight.onActionProperty().set( action );
   }

   private void action_exit ( ActionEvent evt ) {
      loader.stop();
      shutdown();
      Platform.exit();
   }

   private void action_stop ( ActionEvent evt ) {
      setStatus( "Stopping" );
      loader.stop();
   }

   private void action_download ( ActionEvent evt ) {
      if ( txtUser.getText().trim().isEmpty() || txtPass.getText().trim().isEmpty() ) {
         new Alert( Alert.AlertType.ERROR, "Please input DDI username and password", ButtonType.OK ).showAndWait();
         if ( txtUser.getText().trim().isEmpty() ) txtUser.requestFocus();
         else txtPass.requestFocus();
         return;
      }
      setStatus( "Starting download" );
      loader.startDownload();
      stateRunning();
   }

   private void action_view ( ActionEvent evt ) {
      File f = getLastExport();
      if ( f == null ) {
         new Alert( Alert.AlertType.ERROR, "Last export may have been deleted", ButtonType.OK ).show();
         stateCanExport( null );
      } else { try {
         Desktop.getDesktop().browse( f.toURI() );
      } catch ( IOException ex ) {
         log.log( Level.WARNING, "Error opening last export {0}: {1}", new Object[]{ f, Utils.stacktrace( ex ) } );
         setStatus( "Error opening last exported file" );
      } }
   }

   private File getLastExport () {
      String path = prefs.get( "export.last_file", null );
      if ( path == null ) return null;
      File lastExport = new File( path );
      if ( ! lastExport.exists() || ! lastExport.isFile() )
         return null;
      return lastExport;
   }


   private FileChooser dlgCreateView;

   private void action_export ( ActionEvent evt ) {
      // Create file dialog
      if ( dlgCreateView == null )
         dlgCreateView = createExportDialog( "4e Offline Compendium", "4e_database.html" );
      File target = dlgCreateView.showSaveDialog( getWindow() );
      if ( target == null || ! target.getName().toLowerCase().endsWith( ".html" ) ) return;
      dlgCreateView.setInitialFileName( target.getName() );

      CompletableFuture<Void> ready = CompletableFuture.completedFuture( null );
      String data_dir = target.toString().replaceAll( "\\.html$", "" ) + "_files/";
      if ( loader.hasOldExport( data_dir ) ) {
         Alert dlgRemoveOld = new Alert( Alert.AlertType.CONFIRMATION, "Delete old version export data?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL );
         ButtonType remove = dlgRemoveOld.showAndWait().orElse( null );
         if ( remove == null || ButtonType.CANCEL.equals( remove ) ) return;
         if ( ButtonType.YES.equals( remove ) )
            ready = loader.deleteOldExport( data_dir );
      }

      ready.thenRun( () -> {
         prefs.remove( "export.last_file" );
         loader.startExport( target ).thenRun( () -> prefs.put( "export.last_file", target.toString() ) );
         prefs.put( "export.dir", target.getParent() );
      } );
   }

   private FileChooser dlgExportRaw;

   private void action_export_raw ( ActionEvent evt ) {
      if ( dlgExportRaw == null ) {
         dlgExportRaw = createExportDialog( "dummy", "*.*" );
         dlgExportRaw.getExtensionFilters().clear();
         dlgExportRaw.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter( "Excel", "*.xlsx" ),
            new FileChooser.ExtensionFilter( "CSV (fixed filenames)", "race.csv" ),
            new FileChooser.ExtensionFilter( "TSV (fixed filenames)", "race.tsv" ),
            new FileChooser.ExtensionFilter( "JSON", "*.json" ),
            new FileChooser.ExtensionFilter( "SQL", "*.sql" ),
            new FileChooser.ExtensionFilter( "HTML", "*.html", "*.htm" ) );
         dlgExportRaw.setInitialFileName( "raw_compendium.xlsx" );
      }
      File target = dlgExportRaw.showSaveDialog( getWindow() );
      if ( target == null ) return;
      dlgExportRaw.setInitialFileName( target.getName() );
      try {
         loader.startExportRaw( target );
         pnlC.getSelectionModel().select( tabData );
      } catch ( RuntimeException err ) {
         log.log( Level.INFO, "Cannot initiate dump: {0}", Utils.stacktrace( err ) );
      }
   }

   private FileChooser createExportDialog ( String display, String filename ) {
      FileChooser dialog = new FileChooser();
      dialog.getExtensionFilters().addAll(
         new FileChooser.ExtensionFilter( display, filename ),
         new FileChooser.ExtensionFilter( "Any html file", "*.html" ) );
      File initialDir = new File( prefs.get( "export.dir", System.getProperty( "user.home" ) ) );
      if ( ! initialDir.exists() || ! initialDir.isDirectory() )
         initialDir = new File( System.getProperty( "user.home" ) );
      dialog.setInitialDirectory( initialDir );
      dialog.setInitialFileName( filename );
      return dialog;
   }

   public void stateBusy ( String message ) { runFX( () -> {
      if ( message != null ) setStatus( message );
      log.log( Level.FINE, "State: Busy" );
      disallowAction();
      setRight( "Exit", this::action_exit );
   } ); }

   public void stateBadData () { runFX( () -> {
      log.log( Level.FINE, "State: Bad Data" );
      setStatus( "Cannot open local database" );
      allowAction();
      setLeft( "Reset", this::btnClearData_click );
   } ); }

   public void stateCanDownload ( String status ) { runFX( () -> {
      log.log( Level.FINE, "State: Can Download" );
      setStatus( status );
      allowAction();
      setLeft( "Download", this::action_download );
      setRight( "Exit", this::action_exit );
   } ); }

   public void stateCanExport ( String status ) { runFX( () -> {
      log.log( Level.FINE, "State: Can Export" );
      setStatus( status );
      allowAction();
      setLeft( "Export", this::action_export );
      File f = getLastExport();
      if ( f == null )
         setRight( "Exit", this::action_exit );
      else
         setRight( "View", this::action_view );
   } ); }

   public void stateRunning () { runFX( () -> {
      log.log( Level.FINE, "State: Running" );
      txtUser.setDisable( true );
      txtPass.setDisable( true );
      chkFixAndEnhance.setDisable( true );
      btnClearData.setDisable( true );
      btnExportData.setDisable( true );
      setLeft( "Stop", this::action_stop );
   } ); }

   public String getUsername () { return txtUser.getText().trim(); }
   public String getPassword () { return txtPass.getText().trim(); }
   public void focusUsername () { txtUser.requestFocus(); };

   /////////////////////////////////////////////////////////////////////////////
   // Option Tab
   /////////////////////////////////////////////////////////////////////////////

   private void chkFix_change ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
      prefs.putBoolean( "export.fix", newValue );
      Controller.fixData = newValue;
   }

   private void chkCompress_change ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
      prefs.putBoolean( "export.compress", newValue );
      ExporterMain.compress.set( newValue );
   }

   private void chkDebug_change ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
      prefs.putBoolean( "gui.debug", newValue );
      Main.debug.set( newValue );
      ObservableList<Tab> tabs = pnlC.getTabs();
      if ( newValue ) {
         log.setLevel( Level.FINE );
         if ( ! tabs.contains( tabLog ) ) {
            int index = tabs.indexOf( tabOption )+1;
            tabs.add( index, tabLog );
            tabs.add( index+1, tabWorker );
         }
      } else {
         log.setLevel( Level.CONFIG );
         tabs.removeAll( tabLog, tabWorker );
         //txtLog.clear();
         //pnlWorker.getConsoleOutput().clear();
         //log.config( "Log cleared by switching off debug." );
      }
   }

   Alert confirmClear;
   private void btnClearData_click ( ActionEvent evt ) {
      if ( confirmClear == null ) {
         confirmClear = JavaFX.dialogDefault( new Alert( Alert.AlertType.CONFIRMATION, "Clear all downloaded data?", ButtonType.YES, ButtonType.NO ), ButtonType.NO );
      }
      final ButtonType result = confirmClear.showAndWait().get();
      if ( ! ButtonType.YES.equals( result ) )
         return;

      stateBusy( null );
      loader.resetDb();
      pnlC.getSelectionModel().select( tabData );
   }

   private void btnCheckUpdate_click ( ActionEvent evt ) {
      checkUpdate( true );
   }

   private void checkUpdate ( boolean forced ) {
      btnCheckUpdate.setText( "Checking update" );
      btnCheckUpdate.setDisable( true );
      Main.checkUpdate( forced ).thenAccept( ( hasUpdate ) ->
         runFX( () -> {
            if ( ! hasUpdate.isPresent() ) {
               btnCheckUpdate.setText( "Check update" );
            } else if ( hasUpdate.get() ) {
               if ( new Alert( Alert.AlertType.INFORMATION, "Update available. Open download page?", ButtonType.YES, ButtonType.CLOSE ).showAndWait().get().equals( ButtonType.YES ) )
                  Main.doUpdate();
               btnCheckUpdate.setText( "Open update page" );
               btnCheckUpdate.addEventHandler( ActionEvent.ACTION, ( evt ) -> Main.doUpdate() );
            } else {
               btnCheckUpdate.setText( "Check update (no update)" );
            }
            btnCheckUpdate.setDisable( false );
         } )
      ).exceptionally( ( ex ) -> {
         runFX( () -> {
            btnCheckUpdate.setText( "Check update (error)" );
            btnCheckUpdate.setDisable( false );
         } );
         return null;
      } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Worker Screen
   /////////////////////////////////////////////////////////////////////////////

   private synchronized void initWorkerTab() {
      if ( pnlWorker != null ) return;
      log.log( Level.INFO, "Initialise download browser" );
      pnlWorker = new ConsoleWebView();
      tabWorker.setContent( new BorderPane( pnlWorker ) );
   }

   public ConsoleWebView getWorker () {
      initWorkerTab();
      return pnlWorker;
   }
}