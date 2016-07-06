package db4e;

import db4e.data.Category;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import sheepy.util.JavaFX;
import sheepy.util.ui.ConsoleWebView;

public class SceneMain extends Scene {

   // System utilities
   public final Logger log = Main.log;
   public final Preferences prefs = Main.prefs;

   // Help Screen
   private final BorderPane pnlHelpTab = new BorderPane();
   private final Tab tabHelp = new Tab( "Help", pnlHelpTab );

   // About Screen
   private final BorderPane pnlAboutTab = new BorderPane();
   private final Tab tabAbout = new Tab( "About", pnlAboutTab );

   // Data Screen
   private final Label lblStatus = new Label( "Starting Up");
   private final TextField txtEmail  = new TextField( prefs.get( "ddi.email", "" ) );
   private final TextField txtPass   = new TextField( prefs.get( "ddi.pass", "" ) );
   private final TableView<Category> tblCategory = new TableView<>();
   private final Button btnView = new Button( "View Data" );
   private final Button btnExport = new Button( "Export Data" );
   private final Button btnStartStop = new Button( "Start Download" );

   private final Pane pnlDataTab = new BorderPane( tblCategory,
      JavaFX.fitVBox( lblStatus, JavaFX.fitHBox( txtEmail, txtPass ) ),  // Top
      null, JavaFX.fitVBox( JavaFX.fitHBox( btnView, btnExport ), btnStartStop ), null ); // right, bottom, left
   private final Tab tabData = new Tab( "Data", pnlDataTab );

   // Option Screen
   private final CheckBox chkDebug = new CheckBox( "Show debug tabs" );
   private final Pane pnlOptionTab = new HBox( chkDebug );
   private final Tab tabOption = new Tab( "Options", pnlOptionTab );

   // Log Screen
   private final TextArea txtLog = new TextArea();
   private final Tab tabLog = new Tab( "Log", new BorderPane( txtLog ) );

   // Worker Screen
   private final ConsoleWebView pnlWorker = new ConsoleWebView();
   private final Tab tabWorker = new Tab( "Worker", new BorderPane( pnlWorker ) );

   // Layout regions
   private final TabPane pnlC = new TabPane( tabHelp, tabData, tabOption, tabAbout );

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

      // Option tab
      chkDebug.selectedProperty().addListener( this::chkDebug_change );
      if ( prefs.getBoolean( "gui.debug", false ) )
         chkDebug.selectedProperty().set( true );

      // Log tab
      txtLog.setEditable( false );

      disableButtons( "Initialising" );
   }

   private void initLayout () {
      Insets i8 = new Insets( 8 );

      pnlDataTab.setPadding( i8 );
         lblStatus.setFont( new Font( lblStatus.getFont().getName(), 24 ) );
         lblStatus.setAlignment( Pos.CENTER );
         lblStatus.setPadding( i8 );
         btnView.setPadding( i8 );
         btnExport.setPadding( i8 );
         btnStartStop.setPadding( i8 );

      pnlOptionTab.setPadding( i8 );
   }

   private void initTabs () {
      ObservableList<Tab> tabs = pnlC.getTabs();
      for ( Tab tab : tabs )
         tab.setClosable( false );
      tabLog.setClosable( false );

      // Start at data tab
      pnlC.getSelectionModel().select( 1 );
      // Load help doc dynamically
      pnlC.getSelectionModel().selectedItemProperty().addListener( (prop,old,now) -> {
         if ( now == tabHelp )
            initWebViewTab( pnlHelpTab );
         else if ( now == tabWorker )
            Platform.runLater( () -> pnlWorker.getConsoleInput().requestFocus() );
         else if ( now == tabAbout )
            initWebViewTab( pnlAboutTab );
      } );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private void setStatus ( String msg ) {
      if ( Platform.isFxApplicationThread() ) {
         lblStatus.setText( msg );
      } else {
         Platform.runLater( () -> setStatus( msg ) );
      }
   }

   private void disableButtons( String status ) {
      if ( Platform.isFxApplicationThread() ) {
         if ( status != null ) {
            setStatus( status );
            log.log( Level.FINE, "{0}, disabling controls", status );
         } else
            log.log( Level.FINE, "Disabling controls" );
         txtEmail.setDisable( true );
         txtPass.setDisable( true );
         btnView.setDisable( true );
         btnExport.setDisable( true );
         btnStartStop.setDisable( true );
      } else {
         Platform.runLater( () -> disableButtons( status ) );
      }
   }

   private void enableButtons( String status ) {
      if ( Platform.isFxApplicationThread() ) {
         if ( status != null ) {
            setStatus( status );
            log.log( Level.FINE, "{0}, enabling controls", status );
         } else
            log.log( Level.FINE, "Enabling controls" );
         txtEmail.setDisable( true );
         txtPass.setDisable( true );
         btnView.setDisable( true );
         btnExport.setDisable( true );
         btnStartStop.setDisable( true );
      } else {
         Platform.runLater( () -> disableButtons( status ) );
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Help & About
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Create a webview, put it in the pane, and load its content async.
    * If the pane already has a Node at the center, this method will do nothing.
    *
    * @param pane Panel to add webview to
    * @return Created webview
    */
   private void initWebViewTab ( BorderPane pane ) {
      if ( pane.getCenter() != null ) return;
      WebView web = new WebView();
      pane.setCenter( web );
      web.getEngine().loadContent( "<h1>Loading</h1>" );
      new Thread( () -> {
         final String txt = "Work in progress"; // TODO: Load from file
         Platform.runLater( () -> {
            web.getEngine().loadContent( txt );
         } );
      } ).start();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Data Tab
   /////////////////////////////////////////////////////////////////////////////

   private FileChooser dlgCreateView;
   public void btnFolder_action ( ActionEvent evt ) {
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

   public void chkDebug_change ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
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
         log.setLevel( Level.INFO );
         if ( tabs.contains( tabLog ) )
            tabs.removeAll( tabLog, tabWorker );
      }
   }

   Alert confirmClear;
   public void btnClear_action ( ActionEvent evt ) {
      assert( Platform.isFxApplicationThread() );

      if ( confirmClear == null ) {
         confirmClear = JavaFX.dialogDefault( new Alert( Alert.AlertType.CONFIRMATION, "Clear all downloaded data?", ButtonType.YES, ButtonType.NO ), ButtonType.NO );
      }
      if ( ! ButtonType.YES.equals( confirmClear.showAndWait().get() ) )
         return;
   }

}