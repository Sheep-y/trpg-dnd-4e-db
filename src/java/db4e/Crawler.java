package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

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

   void getCategoryData ( Category cat ) throws InterruptedException, TimeoutException {
      eval( " document.querySelector( '[name=endPos]' ).setAttribute( 'select', \"'99999'\" ) " ); // Update XSL's limit
      browse( "http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab=" + cat.id );
   }

   List<Entry> openCategory () throws InterruptedException, TimeoutException {

      Object data = eval(
           " var links = document.querySelectorAll( 'a:not([href^=javascript])' ),  result = []; "
         + " for ( var y = 0, max_y = links.length ; y < max_y ; y++ ) { "
         + "    var a = links[ y ],  cells = a.parentNode.parentNode.cells,  prop = []; "
         //     meta = [  [ id , name , [ meta properties ] ] , ... ]
         + "    var row = [ a.href, a.textContent.trim(), prop ]; "
         + "    for ( var x = 1, max_x = cells.length ; x < max_x ; x++ ) "
         + "       prop.push( cells[ x ].textContent.trim() ); "
         + "    result.push( row ); "
         + " } result; " );

      Object[] rows = toArray( data );
      List<Entry> result = new ArrayList<>( rows.length );
      for ( Object line : rows ) {
         Object[] row = toArray( line );
         String name = row[1].toString();
         log.log( Level.FINER, "Copying row {0}", name );
         Object[] props = toArray( row[ 2 ] );
         String[] fields = Arrays.copyOf( props, props.length, String[].class);
         result.add( new Entry( row[0].toString(), name, fields ) );
      }
      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////
   /**
    * Convert an array-like JSObject into Object[].
    *
    * @param jsobj JSObject to convert
    * @return Result Object[]
    * @throws NullPointerException if jsobj is null or does not have length property
    */
   private Object[] toArray( Object jsobj ) {
      JSObject obj = (JSObject) jsobj;
      int length = ( ( Number ) obj.getMember( "length" ) ).intValue();
      Object[] result = new Object[ length ];
      for ( int i = 0 ; i < length ; i++ )
         result[ i ] = obj.getSlot( i );
      return result;
   }

   /**
    * Run a JavaScript and return its result.
    * Well, it's a little bit more complicated than it sounds.
    *
    * @param script Script to run on browser
    * @return Script result
    * @throws InterruptedException If interrupted during execution
    */
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

}