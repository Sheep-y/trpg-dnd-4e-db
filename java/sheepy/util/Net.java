package sheepy.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import netscape.javascript.JSObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Net {

   /**
    * Call this function to trust all certificates in future SSL connections.
    */
   public static void trustAllSSL () {
      synchronized ( TrustEveryoneManager.class ) {
         if ( trustedAll ) return;
         try {
             SSLContext sc = SSLContext.getInstance( "SSL" );
             sc.init( null, new TrustManager[]{ new TrustEveryoneManager() }, new SecureRandom() );
             HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );
             trustedAll = true;
         } catch ( NoSuchAlgorithmException | KeyManagementException ex ) {
            throw new RuntimeException( ex );
         }
      }
   }

   private static boolean trustedAll = false;
   private static class TrustEveryoneManager implements X509TrustManager {
       @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
       @Override public void checkClientTrusted( X509Certificate[] certs, String authType ) { }
       @Override public void checkServerTrusted( X509Certificate[] certs, String authType ) { }
   }

   public static Object define ( Object value ) {
      return Objects.equals( "undefined", value ) ? null : value;
   }

   /**
    * Take a JSObject and return console.log style representation.
    *
    * - String will be displayed quoted.
    * - Nodes will be displayed in #text or in bracket.
    * - Array like objects will be expanded.
    *
    * @param o Subject
    * @return JS console string representation.
    */
   public static CharSequence toString( Object o ) {
      if ( o == null ) return "null";
      if ( o instanceof String ) return "undefined".equals( o ) ? o.toString() : '"' + o.toString() + '"';
      if ( o instanceof Node ) return nodeToString( (Node) o );
      if ( o instanceof JSObject ) {
         JSObject obj = (JSObject) o;
         final Object val = obj.getMember( "length" );
         if ( val != null && val instanceof Number ) { // Array like?
            final Object apply = define( obj.getMember( "apply" ) );
            if ( apply != null ) {
               return Objects.toString( obj );
            }
            StringBuilder str = new StringBuilder().append( '[' );
            for ( int i = 0, len = ((Number)val).intValue() ; i < len ; i++ ) {
               if ( i > 0 ) str.append( ',' );
               if ( i == 100 ) {
                  str.append( "and " ).append( len-i ).append( " more" );
                  break;
               }
               Object e = obj.getSlot( i );
               if ( e != null ) str.append( toString( e ) );
            }
            return str.append( ']' );
         }
      }
      return o.toString();
      //return o.getClass().toString();
   }

   /**
    * Given a DOM node, produce a string representation based on id and className.
    *
    * @param node Subject node.
    * @return String representing the node.
    */
   public static CharSequence nodeToString ( Node node ) {
      switch ( node.getNodeType() ) {
         case Node.ELEMENT_NODE:
            StringBuilder str = new StringBuilder().append( '<' );
            str.append( node.getNodeName().toLowerCase() );
            if ( node.hasAttributes() ) {
               NamedNodeMap list = node.getAttributes();
               if ( list.getNamedItem( "id" ) != null )
                  str.append( " id=\"" ).append( list.getNamedItem( "id" ).getTextContent() ).append( "\"" );
               if ( list.getNamedItem( "class" ) != null )
                  str.append( " class=\"" ).append( list.getNamedItem( "class" ).getTextContent() ).append( "\"" );
            }
            return str.append( '>' );

         case Node.TEXT_NODE:
         case Node.CDATA_SECTION_NODE:
            return "#text \"" + node.getNodeValue().replace( '\n', ' ' ).replace( '\r', ' ' ) + "\"";

         case Node.COMMENT_NODE:
            return "<!--" + node.getNodeValue().replace( '\n', ' ' ).replace( '\r', ' ' ) + "-->";

         case Node.DOCUMENT_FRAGMENT_NODE:
            return "<!DOCTYPE " + node.getNodeName() + ">";

         default:
            return Objects.toString( node );
      }
   }

   /**
    * Console functional interface
    */
   public abstract static class Console {
      protected Map<String,Long> timer;
      public void group() {}
      public void groupCollapsed() {}
      public void groupEnd() {}
      public void trace() { new Exception( "Stack trace" ).printStackTrace(); }
      public void log  ( Object args ) { handle( Level.INFO, args ); }
      public void debug( Object args ) { handle( Level.FINE, args ); }
      public void info ( Object args ) { handle( Level.CONFIG, args ); }
      public void warn ( Object args ) { handle( Level.WARNING, args ); }
      public void error( Object args ) { handle( Level.SEVERE, args ); }
      public void time ( String args ) {
         if ( args == null ) return;
         synchronized ( this ) { if ( timer == null ) timer = new HashMap<>(); }
         synchronized ( timer ) { timer.put( args, System.nanoTime() ); }
      }
      public void timeEnd( String args ) {
         if ( args == null ) return;
         long ns;
         synchronized( this ) { if ( timer == null ) return; }
         synchronized( timer ) {
            if ( ! timer.containsKey( args ) ) return;
            ns = System.nanoTime() - timer.remove( args );
         }
         handle( Level.INFO, args + ": " + (ns/1_000_000) + "ms" );
      }
      public abstract void handle( Level level, Object args );
   }

   /**
    * A console that logs to System.out or System.err.
    * log, debug, and info goes to System.out, warn and error goes to System.err.
    */
   public static class ConsoleSystem extends Console {
      @Override public void handle ( Level level, Object args ) {
         if ( Level.SEVERE.intValue() < level.intValue() )
            System.out.println( Objects.toString( args ) );
         else
            System.err.println( Objects.toString( args ) );
      }
   }

   /**
    * A console that logs to a Java logger
    */
   public static class ConsoleLogger extends Console {
      private final Logger log;
      public ConsoleLogger ( Logger log ) { this.log = log; }
      @Override public void handle ( Level level, Object args ) {
         log.log( level, Objects.toString( args ) );
      }
   }
}