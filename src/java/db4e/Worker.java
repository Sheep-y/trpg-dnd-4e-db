package db4e;

import java.time.Period;
import java.util.logging.Level;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import sheepy.util.Net;

public class Worker {

   private final Button btnRerun = new Button( "" );
   private final WebView web = new WebView();
   private final WebEngine js = web.getEngine();

   private final FlowPane pnlT = new FlowPane( btnRerun );
   private final BorderPane panel = new BorderPane( web, pnlT, null, null, null );

   private final Downloader main;
   private ActionStrategy action = new ActionLoadAgent();

   /*
   private enum STATE {
      LOADING_AGENT,
      LOADING_HOME,
      LOADING_LIST,
      LOGIN, // Waiting for user login
      READY,
      FAILED,
   };
   */

   static {
      Net.trustAllSSL(); // Kill invalid SSL errors in advance
   }

   public Worker( Downloader main ) {
      this.main = main;
      btnRerun.setDisable( true );
      js.setOnAlert( e -> new Alert( Alert.AlertType.INFORMATION, e.getData().toString(), ButtonType.OK ).showAndWait() );
      js.setOnError( e -> main.log.log( Level.WARNING, "log.web.error", Utils.stacktrace( e.getException() ) ) );
      js.getLoadWorker().stateProperty().addListener( (prop,old,now) -> {
         switch ( now ) {
            case SUCCEEDED:
               action.succeed();
               break;
            case FAILED: // Excludes cancelled
               action.error();
         }
      } );
      action.run();
   }

   public Node getPanel() {
      return panel;
   }

   /*****************************************************
    * Strategies
    *****************************************************/ //

   private void runAction( ActionStrategy act ) {
      action = act;
      act.run();
   }

   private abstract class ActionStrategy {
      void run() {
         main.log.log( Level.FINE, "log.web.run", this.getClass().getSimpleName() );
      }
      void succeed() {
         btnRerun.setDisable( true );
      }
      void error() {
         runAction( new ActionError() );
      }
      /**
       * Utility method: get properties from a DOM element list
       */
      protected String[] queryProperty( String selector, String property ) {
         Object txt = Net.define( js.executeScript(
               "[].slice.call( document.querySelectorAll('" + selector + "'), 0 ).map( function(e){ return e." + property + "; } ).join('、')"
            ) );
         if ( txt == null ) return new String[]{};
         return txt.toString().split( "、" );
      }
   }

   /*****************************************************
    * Display engine error
    *****************************************************/
   private class ActionError extends ActionStrategy {
      @Override void run() {
         super.run();

         // Get message
         javafx.concurrent.Worker<Void> worker = js.getLoadWorker();
         String msg = worker.getMessage();
         Throwable ex = worker.getException();

         // Compose html
         String html = "<h1>Error " + msg + "</h1>";
         if ( ex != null )
            html += "<pre>" + Utils.stacktrace( ex ) + "</pre>";

         // Display composed error screen
         js.loadContent( html );
      }

      @Override void succeed() {
         btnRerun.setDisable( false );
      }
      @Override void error() {
         succeed();
      }
   }

   /*****************************************************
    * Load latest Chrome as user agent
    *****************************************************/
   private class ActionLoadAgent extends ActionStrategy {
      @Override void run() {
         super.run();

         // Update only if Agent has not been loaded
         if ( js.getUserAgent().contains( "JavaFX" ) ) {
            String userAgent = Utils.getMortalPref( main.prefs, "worker.agent" );
            if ( userAgent == null ) {
               // Load online agent list and continue at this.succeed or this.error
               js.load( "http://www.useragentstring.com/pages/Chrome/" );
               return;
            }
            js.setUserAgent( userAgent );
         }
         // Agent is set.  Continue to next step.
         runAction( new ActionLoadCat() );
      }

      @Override void succeed() {
         super.succeed();
         Object agent = Net.define( js.executeScript( "document.querySelector( 'ul a' ).textContent" ) );
         if ( agent != null && agent.toString().toLowerCase().contains( "webkit" ) )
            setAgent( agent.toString() );
         else
            error();
      }

      @Override void error() {
         // Use MSIE 11 which, being the last IE, should last quite a few years
         setAgent( "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko" );
      }

      private void setAgent( String agent ) {
         js.setUserAgent( agent );
         Utils.putMortalPref( main.prefs, "worker.agent", agent, Period.ofDays( 7 ) );
         runAction( new ActionLoadCat() );
      }
   }

   /*****************************************************
    * Load Category list
    *****************************************************/
   private class ActionLoadCat extends ActionStrategy {
      @Override void run() {
         super.run();
//         js.load( "http://www.wizards.com/dndinsider/compendium/database.aspx" );
      }

      @Override void succeed() {
         super.succeed();
         String[] list = queryProperty( "#category option", "value" );
         main.remote.addCategories( list );
         runAction( new ActionCheckLogin() );
      }
   }

   /*****************************************************
    * Check login status with random glossary
    *****************************************************/
   private class ActionCheckLogin extends ActionStrategy {
      private String[] glossaryId = null;

      @Override void run() {
         super.run();
      }

      @Override void succeed() {
         super.succeed();
         String[] list = queryProperty( "ID", "textContent" );
         for ( String id : list ) {
         }
      }
   }
   // xsl: http://www.wizards.com/dndinsider/compendium/xsl/Glossary.xsl
   // data: http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab=Glossary
}