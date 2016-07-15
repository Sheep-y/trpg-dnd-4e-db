package db4e;

import db4e.data.Category;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import sheepy.util.JavaFX;
import sheepy.util.ResourceUtils;
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
   private final Label lblStatus = new Label( "Starting Up");
   private final TextField txtEmail  = JavaFX.tooltip( new TextField( prefs.get( "ddi.email", "" ) ),
           "DDI subscriber email." );
   private final TextField txtPass   = JavaFX.tooltip( new TextField( prefs.get( "ddi.pass", "" ) ),
           "DDI subscriber password." );
   private final TableView<Category> tblCategory = new TableView<>();
      private final TableColumn<Category,String > colName = new TableColumn<>( "Category" );
      private final TableColumn<Category,Integer> colTotalEntry = new TableColumn<>( "Total" );
      private final TableColumn<Category,Integer> colDownloadedEntry = new TableColumn<>( "Downloaded" );
      private final TableColumn<Category,Integer> colExportedEntry = new TableColumn<>( "Saved" );
   private final Button btnLeft = new Button( "Please" );
   private final Button btnRight = new Button( "Wait" );

   private final Pane pnlDataTab = new BorderPane( tblCategory,
      JavaFX.fitVBox( lblStatus, JavaFX.fitHBox( txtEmail, txtPass ) ),  // Top
      null, JavaFX.fitHBox( btnLeft, btnRight ), null ); // right, bottom, left
   private final Tab tabData = new Tab( "Data", pnlDataTab );

   // Option Screen
   private final CheckBox chkDebug = JavaFX.tooltip( new CheckBox( "Show debug tabs" ),
           "Show app log and console.  Increase memoro usage because of finer logging level." );
   final Button btnClearData = JavaFX.tooltip( new Button( "Clear Downloaded Data" ), // Allow downloader access, to allow clear when db is down
           "Clear ALL downloaded data by deleting '" + Downloader.DB_NAME + "'." );
   private final Pane pnlOptionTab = new VBox( 8, chkDebug, btnClearData );
   private final Tab tabOption = new Tab( "Options", pnlOptionTab );

   // Log Screen
   private final TextArea txtLog = new TextArea();
   private final Tab tabLog = new Tab( "Log", new BorderPane( txtLog ) );

   // Worker Screen
   private final ConsoleWebView pnlWorker = new ConsoleWebView();
   private final Tab tabWorker = new Tab( "Worker", new BorderPane( pnlWorker ) );
   private final Downloader loader = new Downloader( this );

   // Layout regions
   private final TabPane pnlC = new TabPane( tabData, tabOption, tabHelp );

   public SceneMain( Main main ) {
      super( new Group(), 640, 450 );
      main.addLoggerOutput( txtLog );
      initControls();
      initLayout();
      initTabs();
      setRoot( pnlC );
   }

   private void initControls () {
      // Data tab - save preference on change
      txtEmail.setPromptText( "DDI login email" );
      txtPass .setPromptText( "DDI login password" );
      txtEmail.textProperty().addListener( (prop, old, now ) -> { prefs.put( "ddi.email", now ); });
      txtPass .textProperty().addListener( (prop, old, now ) -> { prefs.put( "ddi.pass" , now ); });

      colName.setCellValueFactory( new PropertyValueFactory( "name" ) );
      colTotalEntry.setCellValueFactory( new PropertyValueFactory<>( "totalEntry" ) );
      colDownloadedEntry.setCellValueFactory( new PropertyValueFactory<>( "downloadedEntry" ) );
      colExportedEntry.setCellValueFactory( new PropertyValueFactory<>( "exportedEntry" ) );
      tblCategory.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
      tblCategory.getColumns().addAll( colName, colDownloadedEntry, colTotalEntry );

      // Option tab
      chkDebug.selectedProperty().addListener( this::chkDebug_change );
      if ( prefs.getBoolean( "gui.debug", false ) )
         chkDebug.selectedProperty().set( true );
      btnClearData.addEventHandler( ActionEvent.ACTION, this::btnClearData_click );

      // Log tab
      txtLog.setEditable( false );

      stateBusy( "Initialising" );
   }

   private void initLayout () {
      Insets i8 = new Insets( 8 );

      pnlDataTab.setPadding( i8 );
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
         if ( now == tabHelp )
            initWebViewTab( pnlHelpTab, "res/downloader_about.html" );
         else if ( now == tabWorker )
            Platform.runLater( () -> pnlWorker.getConsoleInput().requestFocus() );
      } );
   }

   // Called by Main after stage show
   void startup() {
      loader.open( tblCategory ).thenRun( () -> Platform.runLater( () ->
         btnLeft.requestFocus()
      ) );
   }

   // Called by Main during stage shutdown
   void shutdown() {
      loader.close();
   }

   /////////////////////////////////////////////////////////////////////////////
   // GUI state
   /////////////////////////////////////////////////////////////////////////////

   void setStatus ( String msg ) { runFX( () -> {
      log.log( Level.INFO, "Status: {0}.", msg );
      if ( ! loader.isPausing() )
         lblStatus.setText( msg );
   } ); }

   private void runFX ( Runnable r ) {
      if ( Platform.isFxApplicationThread() ) {
         r.run();
      } else {
         Platform.runLater( r );
      }
   }

   private void allowAction () {
      btnLeft.setDisable( false );
      btnRight.setDisable( false );
      btnClearData.setDisable( false );
   }

   private void disallowAction () {
      btnLeft.setDisable( true );
      btnRight.setDisable( true );
      btnClearData.setDisable( true );
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
               log.log( Level.WARNING, "Malformed URL: {0}", err );
               //new Alert( Alert.AlertType.ERROR, "Cannot open " + now, ButtonType.OK ).show();
            } } );
         }
         return popupHandler;
      } );
      new Thread( () -> {
         try {
            final String txt = ResourceUtils.getText( doc );
            runFX( () -> {
                  web.getEngine().loadContent( txt );
            } );
         } catch ( IOException ex ) {
            log.log( Level.WARNING, "Error when loading help: {0}", Utils.stacktrace(ex) );
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
      shutdown();
      Platform.exit();
   }

   private void action_stop ( ActionEvent evt ) {
      loader.stop();
   }

   private void action_download ( ActionEvent evt ) {
      setStatus( "Starting Download" );
      loader.startDownload().whenComplete( (a,b) -> allowAction() );
      stateRunning();
   }

   private void action_pause ( ActionEvent evt ) {
      setStatus( "Pausing" );
      loader.pause();
      statePaused();
   }

   private void action_resume ( ActionEvent evt ) {
      setStatus( "Resuming" );
      loader.resume();
      stateRunning();
   }

   void stateBusy ( String message ) { runFX( () -> {
      if ( message != null ) setStatus( message );
      log.log( Level.FINE, "State: Busy" );
      disallowAction();
   } ); }

   void stateBadData () { runFX( () -> {
      log.log( Level.FINE, "State: Bad Data" );
      setStatus( "Cannot open local database" );
      allowAction();
      setLeft( "Reset", this::btnClearData_click );
      setRight( "Exit", this::action_exit );
   } ); }

   void stateCanDownload () { runFX( () -> {
      log.log( Level.FINE, "State: Can Download" );
      setStatus( "Ready to download" );
      allowAction();
      setLeft( "Download", this::action_download );
      setRight( "Exit", this::action_exit );
   } ); }

   void stateRunning () { runFX( () -> {
      log.log( Level.FINE, "State: Running" );
      btnClearData.setDisable( true );
      setLeft( "Pause", this::action_pause );
      setRight( "Stop", this::action_stop );
   } ); }

   void statePaused () { runFX( () -> {
      log.log( Level.FINE, "State: Paused" );
      setLeft( "Resume", this::action_resume );
   } ); }

   private FileChooser dlgCreateView;
   private void btnFolder_action ( ActionEvent evt ) {
      assert( Platform.isFxApplicationThread() );

      // Create file dialog
      if ( dlgCreateView == null ) {
         dlgCreateView = new FileChooser();
         dlgCreateView.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter( "4e Offline Compendium", "dd4_database.html" ),
            new FileChooser.ExtensionFilter( "Any file", "*.*" ) );
         dlgCreateView.setInitialDirectory( new File( System.getProperty( "user.home" ) ) );
      }
      dlgCreateView.showSaveDialog( getWindow() );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Option Tab
   /////////////////////////////////////////////////////////////////////////////

   private void chkDebug_change ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
      prefs.putBoolean("gui.debug", newValue );
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
         if ( tabs.contains( tabLog ) )
            tabs.removeAll( tabLog, tabWorker );
         txtLog.clear();
         pnlWorker.getConsoleOutput().clear();
         log.config( "Log cleared by switching off debug." );
      }
   }

   Alert confirmClear;
   private void btnClearData_click( ActionEvent evt ) {
      assert( Platform.isFxApplicationThread() );

      if ( confirmClear == null ) {
         confirmClear = JavaFX.dialogDefault( new Alert( Alert.AlertType.CONFIRMATION, "Clear all downloaded data?", ButtonType.YES, ButtonType.NO ), ButtonType.NO );
      }
      final ButtonType result = confirmClear.showAndWait().get();
      if ( ! ButtonType.YES.equals( result ) )
         return;

      loader.resetDb();
      pnlC.getSelectionModel().select( tabData );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Worker Screen
   /////////////////////////////////////////////////////////////////////////////

   ConsoleWebView getWorker () {
      return pnlWorker;
   }

}