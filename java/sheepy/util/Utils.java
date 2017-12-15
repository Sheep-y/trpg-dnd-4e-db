package sheepy.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class Utils {

   /** Return a copy of the source cloned in a sync block */
   public static <T> List<T> sync( List<T> source ) {
      return sync( source, source );
   }

   /** Return a copy of the source cloned in a sync block */
   public static <T> List<T> sync( List<T> source, Object lock ) {
      synchronized (lock) {
         return new ArrayList<>(source);
      }
   }

   public static String stacktrace ( Throwable ex ) {
      if ( ex == null ) return "";
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace( pw );
      return sw.toString();
   }

   public static TimerTask toTimer ( Runnable task ) {
      return new TimerTask() {
         @Override public void run() {
            task.run();
         }
      };
   }

   public static List<String> matchAll ( Matcher m, String src ) {
      return matchAll( m, src, 0 );
   }

   public static List<String> matchAll ( Matcher m, String src, int group ) {
      List<String> result = new ArrayList<>();
      m.reset( src );
      while ( m.find() ) result.add( m.group( group ) );
      return result;
   }

   public static String ucfirst ( String text ) {
      return Character.toUpperCase( text.charAt( 0 ) ) + text.substring(1);
   }


   private static abstract class Escaper {
      abstract boolean needEscape ( final char chr );
      abstract void doEscape ( StringBuilder out, final char chr );
      abstract int escapeCharCount(); // Number of addition character per escape
      abstract int capacityIncrement(); // How many char to expand the buffer
   }

   /* Logic for escaping HTML string */
   private static final Escaper HtmlEscaper = new Escaper() {
      @Override int escapeCharCount() { return 7; }
      @Override int capacityIncrement() { return 256; }
      @Override boolean needEscape( final char c ) {
         switch ( c ) { case '\'':  case '"' :  case '&' :  case '<' :  case '>' :
            return true;
         }
         return c > 127;
      }
      @Override void doEscape( final StringBuilder out, final char c ) {
         switch ( c ) {
            case '\'': out.append( "&#39;" ); break;
            case '"' : out.append( "&#34;" ); break;
            case '&' : out.append( "&#38;" ); break;
            case '<' : out.append( "&lt;" ); break;
            case '>' : out.append( "&gt;" ); break;
            default:   out.append( "&#" ).append( (int) c ).append( ';' );
         }
      }
   };

   /* Logic for escaping JS string */
   private static final Escaper JsStrEscaper = new Escaper() {
      @Override int escapeCharCount() { return 1; }
      @Override int capacityIncrement() { return 64; }
      @Override boolean needEscape( final char c ) {
         switch ( c ) { case '\b': case '\f': case '\n': case '\r': case '\\': case '\"': case '\u2028': case '\u2029':
            return true;
         }
         return false;
      }
      @Override void doEscape( final StringBuilder out, final char c ) {
         switch ( c ) {
            case '\b': out.append( "\\b" ); break;
            case '\f': out.append( "\\f" ); break;
            case '\n': out.append( "\\n" ); break;
            case '\r': out.append( "\\r" ); break;
            case '\\': out.append( "\\\\" ); break;
            case '\"': out.append( "\\\"" ); break;
            case '\u2028': out.append( "\\u2028" ); break;
            case '\u2029': out.append( "\\u2029" ); break;
            default: throw new IllegalStateException();
         }
      }
   };

   private static String escape ( CharSequence src, Escaper scheme ) {
      StringBuilder out = null;
      int i = 0, last = 0, increment = 0, srclen = src.length();
scan_loop:
      for ( ; i < srclen ; i++ ) {
         if ( scheme.needEscape( src.charAt( i ) ) ) {
            increment = scheme.capacityIncrement();
            out = new StringBuilder( srclen + increment );
            break scan_loop;
         }
      }
      if ( out == null ) return src.toString(); // Quick return if no need to escape.

      final int escapeCharCount = scheme.escapeCharCount();
      for ( ; i < srclen ; i++ ) {
         char c = src.charAt( i );
         if ( scheme.needEscape( c ) ) {
            if ( last < i ) {
               int minLen = out.length() + escapeCharCount + ( srclen - i );
               if ( out.capacity() < minLen )
                  out.ensureCapacity( out.length() + increment + ( srclen - i ) );
               out.append( src, last, i );
            }
            scheme.doEscape( out, c );
            last = i+1;
         }
      }
      if ( last < srclen )
         out.append( src, last, srclen );
      return out.toString();
   }

   /**
    * Safe html text escape ('"<>& and everything above &#127;), suitable for use as property value or content text.
    *
    * @param src Text to escape
    * @return Escaped text.
    */
   public static String escapeHTML ( CharSequence src ) {
      return escape( src, HtmlEscaper );
   }

   /**
    * Js (ES8) text escape, suitable for use as js string content enclosed by double quote (enclosing quotes not expected or included).
    * Single quotes are not escaped for JSON compatibility.
    *
    * @param src Text to escape
    * @return Escaped text.
    */
   public static String escapeJsString ( CharSequence src ) {
      return escape( src, JsStrEscaper );
   }
}