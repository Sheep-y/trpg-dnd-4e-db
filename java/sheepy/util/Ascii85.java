/*
 * libxjava - utility library for cross-Java-platform development
 * Copyright (c) 2010-2011 Marcel Patzlaff (marcel.patzlaff@gmail.com)
 */
package sheepy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.US_ASCII;

/* Base85 (RFC 1924)  encode / decoder */
public class Ascii85 {
   private static final char[] ENCODABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~".toCharArray();
   private static final char[] DECODABET = new char[256];

   private static final long[] FACTORS= {
      1,
      85,
      7225,     // 85^2
      614125,   // 85^3
      52200625  // 85^4
   };

   static {
      for( int i= 0; i < ENCODABET.length; ++i )
         DECODABET[ ENCODABET[i] ] = (char) i;
   }

   static public int calcDecodedLength ( int encoded_length ) {
      return (int) Math.ceil( encoded_length * 0.8 );
   }

   static public int calcEncodedLength ( int decoded_length ) {
      return (int) Math.ceil( decoded_length * 1.2 ) + 5;
   }

   static public byte[] decode ( String data )  {
      try ( ByteArrayOutputStream out = new ByteArrayOutputStream( calcDecodedLength( data.length() ) ) ) {
         decode( new ByteArrayInputStream( data.getBytes( US_ASCII ) ), out );
         return out.toByteArray();
      } catch ( IOException ex ) {
         throw new RuntimeException( "Cannot decode from Base85", ex );
      }
   }

   static public String decode ( String data, Charset charset )  {
      return new String( decode( data ), charset );
   }

   static public void decode ( InputStream in, OutputStream out ) throws IOException {
      int b, factor= 5;
      long sum= 0;
      while ( ( b = in.read() ) >= 0) {
         sum += DECODABET[b] * FACTORS[ --factor ];
         if ( factor == 0) {
            for(int e= 24; e >= 0; e-= 8)
               out.write((int)((sum >>> e) & 0xFF));
            sum = 0;
            factor = 5;
         }
      }
      // process rest (if present)
      if ( factor < 5 ) {
         sum /= FACTORS[factor];
         for( int e = (3 - factor) * 8; e >= 0; e -= 8)
            out.write((int)((sum >>> e) & 0xFF));
      }
   }

   static public String encode ( byte[] data ) {
      return encode( data, 0, data.length );
   }

   static public String encode ( byte[] data, int start, int length ) {
      StringWriter out = new StringWriter( calcEncodedLength( length ) );
      try {
         encode( new ByteArrayInputStream( data, start, length ), out );
      } catch ( IOException ex ) {
         throw new RuntimeException( "Cannot encode to Base85", ex );
      }
      return out.toString();
   }

   static public void encode ( InputStream in, OutputStream out ) throws IOException {
      encode( in, new OutputStreamWriter( out ) );
   }

   static public void encode ( InputStream in, Writer out ) throws IOException {
      int b, bytes = 0;
      long sum = 0;
      while( ( b = in.read() ) >= 0) {
         sum = (sum << 8) | b;
         if ( ++bytes == 4 ) {
            for ( int e = 4 ; e >= 0 ; e-- ) {
               out.write( ENCODABET[ (int) ( sum / FACTORS[e] ) ] );
               sum %= FACTORS[e];
            }
            sum = 0;
            bytes = 0;
         }
      }

      // padding (if present)
      if ( bytes > 0 ) {
         for ( int e = bytes ; e >= 0 ; e-- ) {
            out.write( ENCODABET[ (int) (sum / FACTORS[e]) ] );
            sum %= FACTORS[e];
         }
      }
   }
}