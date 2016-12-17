package sheepy.util.ui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

   final WebView view = new WebView();

   final BorderPane pnlC = new BorderPane();
   final TextField txtInput = new TextField();
   final TextArea txtOutput = new TextArea();
   String last_cmd = "";

   private Consumer<ConsoleWebView> onload;
   private BiConsumer<ConsoleWebView, Throwable> onerror;

   public ConsoleWebView () {
      setOrientation( Orientation.VERTICAL );
      setDividerPositions( 0.8 );
      getItems().addAll(view, new BorderPane( txtOutput, null, null, txtInput, null ) );
      txtOutput.setMinHeight( 100 );

      txtOutput.setEditable( false );
      txtInput.setPromptText( "Browser console input" );
      txtInput.requestFocus();
      txtInput.setOnKeyPressed( e -> {
         if ( e.getCode() == KeyCode.UP )
            txtInput.setText( last_cmd );
      } );

      // Console in/out
      txtInput.setOnAction( e -> {
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
      JavaFX.initWebEngine( view.getEngine(), this::view_handle, log );
   }

   private synchronized void view_handle ( WebEngine view, Throwable error ) {
      if ( error != null ) {
         if ( onerror != null ) onerror.accept( this, error );
      } else {
         if ( onload != null ) onload.accept( this );
      }
   }

   private void log ( String msg ) {
      txtOutput.insertText( txtOutput.lengthProperty().get(), msg );
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

   public synchronized Consumer<ConsoleWebView> getOnload() { return onload; }

   public synchronized void setOnload(Consumer<ConsoleWebView> onload) { this.onload = onload; }

   public synchronized BiConsumer<ConsoleWebView, Throwable> getOnerror() { return onerror; }

   public synchronized void setOnerror(BiConsumer<ConsoleWebView, Throwable> onerror) { this.onerror = onerror; }

   public synchronized void handle( Consumer<ConsoleWebView> onload, BiConsumer<ConsoleWebView, Throwable> onerror) {
      this.onload = onload;
      this.onerror = onerror;
   }
}