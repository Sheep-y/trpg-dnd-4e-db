package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
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
      try {
         eval( " for ( var a of [] ); " ); // Older Java 8 (u45?( does not support for...of
      } catch ( Exception e ) {
         if ( e.getCause() != null && e.getCause().getMessage().contains( "SyntaxError" ) )
            throw new UnsupportedOperationException( "Please upgrade Java" );
      }
      browse( "http://www.wizards.com/dndinsider/compendium/database.aspx" );
   }

   List<Entry> openCategory( Category cat, BiConsumer<Integer,Integer> statusUpdate ) throws InterruptedException, TimeoutException {
      // Command page to get category
      eval( " page_set = null; "
          + " if ( resultsPerPage !== 100 ) SetResultsPerPage( 100 ); " // Set on first tab.  Should be instantaneous in that case.
          + " SwitchTab( '" + cat.id + "' ); " ); // Select tab and enable search button
      Object page_set = waitListData( " $( 'searchbutton' ).click(); ", "page_set ? page_set.length : null" );

      // Check and get item count and thus page count.
      if ( ! ( page_set instanceof Number ) )
         throwAsException( page_set, "Category " + cat.id + " listing" );
      final int itemCount = ( (Number) page_set ).intValue();
      int totalPages = (int) Math.ceil(itemCount / 100.0 );
      if ( totalPages <= 0 )
         throw new AssertionError( "Category '" + cat.name + "' is empty." );
      List<Entry> result = new ArrayList<>( itemCount );
      statusUpdate.accept( 0, itemCount );

      // Get each page and add to result.
      for ( int page = 1 ; page <= totalPages ; page++ ) {
         log.log( Level.FINE, "Category {0} page {1}", new Object[]{ cat.id, page } );
         Object meta = waitListData(
                 " GotoPage(" + page + ");",
                 " (function(){ "
               + "    var meta = [], links = document.querySelectorAll( '.resultsTable a:not([href^=javascript])' ); "
               + "    for ( var a of [].slice.call( links ) ) { "
               + "       var cells = [].slice.call( a.parentNode.parentNode.cells ); "
               + "       var row = [ a.href.replace( 'http://www.wizards.com/dndinsider/compendium/', ''), cells[0].textContent.trim(), [] ]; "
               + "       for ( var td of cells.slice( 1 ) ) "
               + "          row[ 2 ].push( td.textContent.trim() ); "
               + "       meta.push( row ); "
               + "    } "
               + "    return meta;"
               + " })() " );

         Object[] rows = toArray( meta );
         for ( int i = 0 ; i < rows.length ; i++ ) {
            Object[] row = toArray( rows[ i ] );
            Object[] props = toArray( row[ 2 ] );
            String[] fields = Arrays.copyOf( props, props.length, String[].class);
            result.add( new Entry( row[0].toString(), row[1].toString(), fields ) );
         }
         statusUpdate.accept( result.size(), itemCount );
      }
      statusUpdate.accept( result.size(), result.size() );

      if ( result.size() != itemCount )
         log.log( Level.SEVERE, "Category {0} entry count mismatch. Expected {1}, found {2}", new Object[]{cat.id, itemCount, result.size() } );
      return result;
   }

   private Object waitListData ( String action, String result ) throws InterruptedException, TimeoutException {
      eval( " $( 'page_nav_bottom' ).textContent = '-';  $( 'resultsbody' ).innerHTML = '';"
          + action
          + " var timeout = setTimeout( function(){ $( 'page_nav_bottom' ).textContent = 'timeout'; }, " + Downloader.TIMEOUT_MS + " ); " );
      Object output = null;
      JSObject check = null;
      do {
         check = (JSObject) eval( " [ document.querySelector( '.resultsTable a:not([href^=javascript])' ), $( 'page_nav_bottom' ).textContent ] " );
         log.log( Level.FINE, "qs = {0}", check.getSlot( 0 ) );
         log.log( Level.FINE, "nav = {0}", check.getSlot( 1 ) );
         if ( "timeout".equals( check.getSlot( 1 ) ) )
            throw new TimeoutException();
         if ( check.getSlot( 0 ) != null && ! "-".equals( check.getSlot( 1 ) ) ) {
            if ( result == null ) break;
            output = eval( result );
            String logTxt = Objects.toString( output );
            if ( logTxt.length() > 100 ) logTxt = logTxt.substring( 0, 86 ) + "...(" + logTxt.length() + " chars)";
            log.log( Level.FINE, "out = {0}", logTxt );
            if ( output != null ) break;
         }
         Thread.sleep( 200 );
      } while ( true );
      eval( " clearTimeout( timeout ); " );
      return output;
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
    * Throw subject as exception.  If it is "timeout", throw a TimeoutException.
    * Otherwise throw RuntimeException.
    *
    * @param error Error object to throw.
    * @param action Describe the action that throws the error; used in exception message
    * @throws TimeoutException if error is "timeout"
    */
   private void throwAsException( Object error, String action ) throws TimeoutException {
      if ( error instanceof String && "timeout".equals( error ) )
         throw new TimeoutException( action );
      if ( ! ( error instanceof Throwable ) )
         throw new RuntimeException( action + " got (" + error.getClass() + "): " + error );
      else
         throw new RuntimeException( action + " got error",  (Throwable) error );
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