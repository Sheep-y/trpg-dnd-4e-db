package sheepy.util.text;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Comparator;

public class I18n {
   public static final Charset UTF16 = Charset.forName( "UTF-16" );
   public static final Charset UTF8 = Charset.forName( "UTF-8" );
   public static final Charset big5 = Charset.forName( "Big5" );

   public static CharsetDecoder strictDecoder ( Charset charset ) {
      CharsetDecoder result = charset.newDecoder();
      result.onMalformedInput( CodingErrorAction.REPORT );
      result.onUnmappableCharacter( CodingErrorAction.REPORT );
      return result;
   }

   public static CharsetEncoder strictEncoder ( Charset charset ) {
      CharsetEncoder result = charset.newEncoder();
      result.onMalformedInput( CodingErrorAction.REPORT );
      result.onUnmappableCharacter( CodingErrorAction.REPORT );
      return result;
   }

   public static byte[] encode ( CharSequence text, CharsetEncoder encoder ) throws CharacterCodingException {
      if ( text.length() <= 0 ) {
         return new byte[0];
      }
      ByteBuffer buf = encoder.encode( CharBuffer.wrap( text ) );
      byte[] result = new byte[ buf.limit() ];
      buf.get( result );
      return result;
   }

   public static String decode ( byte[] data, CharsetDecoder decoder ) throws CharacterCodingException {
      if ( data.length <= 0 ) {
         return "";
      }
      return decoder.decode( ByteBuffer.wrap( data ) ).toString();
   }

   /*******************************************************************************************************************/
   // Comparator

   public static int compareDefault ( CharSequence a, CharSequence b) {
      return a.toString().compareTo( b.toString() );
   }

   public static int compareBig5Stroke ( CharSequence a, CharSequence b) {
      CharsetEncoder encoder = strictEncoder( big5 );
      try {
         byte[] x    = encode( a, encoder ), y    = encode( b, encoder );
         int    xlen = x.length            , ylen = y.length;
         for ( int i = 0, len = Math.min( x.length, y.length )-1 ; i <= len ; i++ ) {
            byte xc = x[i], yc = y[i];
            if ( xc == yc ) continue;
            return ( xc & 0xFF ) - ( yc & 0xFF ) ; // Compare unsigned
         }
         return xlen - ylen; // Compare length
      } catch ( CharacterCodingException ex ) {
         return compareDefault( a, b );
      }
   }

   public static boolean hasUnifiedCJK ( CharSequence text ) {
      return text.codePoints().anyMatch( i ->
         ( i >= 0x4e00  && i <= 0x9FFF  ) || // CJK Unified Ideographs
         ( i >= 0x3400  && i <= 0x4DBF  ) || // CJK Unified Ideographs Extension A
         ( i >= 0x20000 && i <= 0x2A6DF ) || // CJK Unified Ideographs Extension B
         ( i >= 0x2A700 && i <= 0x2B81F ) ); // CJK Unified Ideographs Extension C && D
   }
   public static boolean hasCJK ( CharSequence text ) {
      return hasUnifiedCJK( text ) || text.codePoints().anyMatch( i ->
         ( i >= 0x3300  && i <= 0x33FF  ) || // CJK Compatibility
         ( i >= 0xFE30  && i <= 0xFE4F  ) || // CJK Compatibility Forms
         ( i >= 0xF900  && i <= 0xFAFF  ) || // CJK Compatibility Ideographs
         ( i >= 0x2F800 && i <= 0x2FA1F )    // CJK Compatibility Ideographs Supplement
      );
   }

   public static Comparator<CharSequence> comparator() {
      return ( a, b ) -> {
         if ( hasCJK( a ) || hasCJK( b ) )
            return compareBig5Stroke( a, b );
         else
            return compareDefault( a, b );
      };
   }
}