package sheepy.util.text;

import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class Base85 {
   // Constants used in encoding and decoding
   private static final long Power5 = 52200625; // 85^5
   private static final long Power4 = 614125;  // 85^4
   private static final long Power3 = 7225;   // 85^3

   public static abstract class Encoder {
      public int calcEncodedLength ( final byte[] data, final int offset, final int length ) {
         return (int) Math.ceil( length * 1.25 );
      }

      public final String encodeToString ( final byte[] data ) {
         return new String( encode( data ), US_ASCII );
      }

      public final byte[] encode ( final byte[] data ) {
         return encode( data, 0, data.length );
      }

      public final byte[] encode ( final byte[] data, final int offset, final int length ) {
         byte[] out = new byte[ calcEncodedLength( data, offset, length ) ];
         _encode( data, 0, data.length, out, 0 );
         return out;
      }

      public final int encode ( final byte[] in, final int in_offset, final int in_length, final byte[] out, final int out_offset ) {
         if ( in_offset < 0 || in_length < 0 || out_offset < 0 || in_offset + in_length > in.length || out_offset > out.length )
            throw new IllegalArgumentException();
         return _encode( in, in_offset, in_length, out, out_offset );
      }

      protected abstract int _encode ( byte[] in, int ri, int rlen, byte[] out, int wi );
   }

   public static class Rfc1924Encoder extends Encoder {
      private static final byte[] EncodeMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~".getBytes( US_ASCII );

      @Override protected int _encode( byte[] in, int ri, int rlen, byte[] out, int wi ) {
         long sum;
         final int loop = rlen / 4;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array();
         for ( int i = loop ; i > 0 ; i-- ) {
            System.arraycopy( in, ri, buf, 0, 4 );
            ri += 4;
            sum = buffer.getInt( 0 ) & 0x00000000ffffffffL;
            out[wi  ] = EncodeMap[ (char) ( sum / Power5 ) ]; sum %= Power5;
            out[wi+1] = EncodeMap[ (char) ( sum / Power4 ) ]; sum %= Power4;
            out[wi+2] = EncodeMap[ (char) ( sum / Power3 ) ]; sum %= Power3;
            out[wi+3] = EncodeMap[ (char) ( sum / 85 ) ];
            out[wi+4] = EncodeMap[ (char) ( sum % 85 ) ];
            wi += 5;
         }
         rlen %= 4;
         if ( rlen == 0 ) return loop * 5;
         sum = 0;
         for ( int i = 0 ; i < rlen ; i++ )
            sum = ( sum << 8 ) + ( in[ri+i] & 0xff );
         switch ( rlen ) {
            case 3: out[wi] = EncodeMap[ (char) ( sum / Power4 ) ]; sum %= Power4; ++wi;
            case 2: out[wi] = EncodeMap[ (char) ( sum / Power3 ) ]; sum %= Power3; ++wi;
         }
         out[wi  ] = EncodeMap[ (char) ( sum / 85 ) ];
         out[wi+1] = EncodeMap[ (char) ( sum % 85 ) ];
         return loop * 5 + rlen + 1;
      }
   }

   private static Encoder RFC1924ENCODER = new Rfc1924Encoder();

   public static Encoder getRfc1942Encoder() { return RFC1924ENCODER; }
}