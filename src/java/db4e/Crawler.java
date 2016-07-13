package db4e;

import db4e.data.Category;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;

/**
 * Responsible for actually crawling compendium website.
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

   void openCategory( Category cat ) {
      eval( " SwitchTab( '" + cat.id + "' ); " );
      eval( " document.querySelector( '#searchbutton' ).click() " );
      Object check = null;
      do {
         sleep( 100 );
         check = eval( " document.querySelectorAll( '.resultsTable a' ).length " );
      } while ( check instanceof Number && ( (Number) check ).intValue() <= 0 );
      if ( ! ( check instanceof Number ) )
         log.log( Level.WARNING, "Category {0} cannot get item count: {1}", new Object[]{ cat.id, check } );
      sleep( 2000 );
   }

   private Object eval ( String script ) {
      return browser.executeScript( script );
   }

   private void sleep( long ms ) throws RuntimeException {
      try {
         Thread.sleep( ms );
      } catch ( InterruptedException ex ) {
         throw new RuntimeException( ex );
      }
   }

}
