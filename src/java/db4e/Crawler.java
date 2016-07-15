package db4e;

import db4e.data.Category;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;

/**
 * Responsible for actually asking browser to navigate and run javascript to get data.
 */
public class Crawler {

   private static final Logger log = Main.log;

   private final WebEngine browser;

   public Crawler ( WebEngine browser ) {
      this.browser = browser;
   }

   private void browse ( String url ) {
      Platform.runLater( () -> browser.load( url ) );
   }

   void openFrontpage () {
      browse( "http://www.wizards.com/dndinsider/compendium/database.aspx" );
   }

   void getCategoryXsl ( Category cat ) {
      browse( "http://www.wizards.com/dndinsider/compendium/xsl/" + cat.id + ".xsl ");
   }

   void getCategoryData ( Category cat ) {
      browse( "http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab=" + cat.id );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Run a JavaScript and return its result.
    * Well, it's a little bit more complicated than it sounds.
    *
    * @param script Script to run on browser
    * @return Script result
    * @throws InterruptedException If interrupted during execution
    *
   private Object eval ( String script ) throws InterruptedException, TimeoutException {
      if ( Platform.isFxApplicationThread() )
         return browser.executeScript( script );

      final Object[] transport = new Object[1];
      synchronized ( transport ) {
         transport[0] = transport;
         Platform.runLater( () -> { synchronized ( transport ) {
            try {
               transport[0] = browser.executeScript( script );
            } catch ( Exception e ) {
               transport[0] = e;
            }
            transport.notify();
         } } );
         transport.wait( Downloader.TIMEOUT_MS );
      }
      if ( transport[0] == transport )
         throw new TimeoutException( "Timeout waiting for '" + script + "'" );
      else if ( transport[0] != null && transport[0] instanceof Exception )
         throw new RuntimeException( "Error running '" + script + "'", (Exception) transport[0] );
      return transport[0];
   }
   */

}