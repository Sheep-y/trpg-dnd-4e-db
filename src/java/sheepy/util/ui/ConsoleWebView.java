package sheepy.util.ui;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import sheepy.util.JavaFX;
import sheepy.util.Net;

public class ConsoleWebView extends SplitPane {

   WebView view = new WebView();

   BorderPane pnlC = new BorderPane();
   TextField txtInput = new TextField();
   TextArea txtOutput = new TextArea();
   String last_cmd = "";

   public ConsoleWebView () {
      setOrientation( Orientation.VERTICAL );
      setDividerPositions( 0.8 );
      getItems().addAll(view, new BorderPane( txtOutput, null, null, txtInput, null ) );
      txtOutput.setMinHeight( 100 );

      txtOutput.setEditable( false );
      txtInput.requestFocus();
      txtInput.setOnKeyPressed(e -> {
         if ( e.getCode() == KeyCode.UP )
            txtInput.setText( last_cmd );
      } );

      // Console in/out
      txtInput.setOnAction(e -> {
         String cmd = last_cmd = txtInput.getText();
         StringBuilder str = new StringBuilder();

         str.append( "> " ).append( cmd ).append( '\n' );
         log( str.toString() );
         str.setLength( 0 );

         Object o;
         try {
            o = view.getEngine().executeScript( cmd );
         } catch ( JSException ex ) {
            o = ex;
         }
         str.append( Net.toString( o ) ).append( '\n' );
         txtInput.clear();
         log( str.toString() );
      });

      // Direct worker logger to console
      Logger log = Logger.getAnonymousLogger();
      log.setLevel( Level.FINE );
      log.addHandler( new ConsoleHandler() );
      JavaFX.initWebEngine( view.getEngine(), null, log );
   }

   private void log ( String msg ) {
      txtOutput.insertText(txtOutput.lengthProperty().get(), msg );
   }

   private class ConsoleHandler extends Handler {
      public ConsoleHandler() {
         setFormatter( new SimpleFormatter() );
         setLevel( Level.ALL );
      }

      @Override public void publish(LogRecord record) {
         log( getFormatter().format( record ) );
      }

      @Override public void flush() {}
      @Override public void close() throws SecurityException {}
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Accessors
   /////////////////////////////////////////////////////////////////////////////

   public WebView getWebView () { return view; }

   public WebEngine getWebEngine () { return view.getEngine(); }

   public TextField getConsoleInput() { return txtInput; }

   public TextArea getConsoleOutput() { return txtOutput; }
}