package sheepy.util;

public class Quote {

   private static abstract class Escaper {
      abstract boolean needEscape ( final char chr );
      abstract void doEscape ( StringBuilder out, final char chr );
      abstract int escapeCharCount(); // Number of addition character per escape
   }

   /* Logic for escaping HTML string */
   private static final Escaper HtmlEscaper = new Escaper() {
      @Override int escapeCharCount() { return 7; }
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
      int i = 0, last = 0, increment = 64, srclen = src.length();
      for ( ; i < srclen ; i++ ) {
         if ( scheme.needEscape( src.charAt( i ) ) ) {
            out = new StringBuilder( srclen + increment );
            break;
         }
      }
      if ( out == null ) return src.toString(); // Quick return if no need to escape.

      final int escapeCharCount = scheme.escapeCharCount();
      for ( ; i < srclen ; i++ ) {
         char c = src.charAt( i );
         if ( scheme.needEscape( c ) ) {
            if ( last < i ) {
               int minLen = out.length() + escapeCharCount + ( srclen - i );
               if ( out.capacity() < minLen ) {
                  increment *= 2;
                  out.ensureCapacity( out.length() + increment + ( srclen - i ) );
               }
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