/*
 * libxjava - utility library for cross-Java-platform development
 *            ${project.name}
 *
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
import static java.nio.charset.StandardCharsets.UTF_8;

/* Base85 (RFC 1924)  encode / decoder */
public class Ascii85 {
    private static final char[] ENCODABET;
    private static final char[] DECODABET;

    private final static long[] FACTORS= {
              1,
             85,
           7225,   // 85^2
         614125,   // 85^3
       52200625    // 85^4
    };

    static {
        ENCODABET= new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '!', '#', '$', '%', '&', '(', ')', '*', '+', '-', ';', '<', '=',
            '>', '?', '@', '^', '_', '`', '{', '|', '}', '~'
        };
        DECODABET= new char[256];
        for( int i= 0; i < ENCODABET.length; ++i )
            DECODABET[ ENCODABET[i] ] = (char) i;
    }


    static public byte[] decode( String data ) throws IOException  {
       ByteArrayOutputStream out = new ByteArrayOutputStream( (int) Math.ceil( data.length() * 0.8 ) );
       decode( new ByteArrayInputStream( data.getBytes( UTF_8 ) ), out );
       return out.toByteArray();
    }

    static public void decode( InputStream in, OutputStream out ) throws IOException {
        int b, factor= 5;
        long sum= 0;
        while((b= in.read()) >= 0) {
            sum+= DECODABET[b] * FACTORS[--factor];
            if(factor == 0) {
                for(int e= 24; e >= 0; e-= 8)
                    out.write((int)((sum >>> e) & 0xFF));
                sum= 0;
                factor= 5;
            }
        }
        // process rest (if present)
        if(factor < 5) {
            sum/= FACTORS[factor];
            for(int e= (3 - factor) * 8; e >= 0; e-= 8)
                out.write((int)((sum >>> e) & 0xFF));
        }
    }

    static public String encode( byte[] data ) throws IOException  {
       StringWriter out = new StringWriter( (int) Math.ceil( data.length * 1.2 ) + 5 );
       encode( new ByteArrayInputStream( data ), out );
       return out.toString();
    }

    static public void encode( InputStream in, OutputStream out ) throws IOException {
       encode( in, new OutputStreamWriter( out ) );
    }

    static public void encode( InputStream in, Writer out ) throws IOException {
        int b, bytes = 0;
        long sum = 0;
        while( ( b = in.read() ) >= 0) {
            sum = (sum << 8) | b;
            bytes++;
            if ( bytes == 4 ) {
                for ( int e = 4 ; e >= 0 ; e-- ) {
                    out.write( ENCODABET[ (int) ( sum / FACTORS[e] ) ] );
                    sum %= FACTORS[e];
                }
                sum= 0;
                bytes= 0;
            }
        }

        // process rest (if present)
        if ( bytes > 0 ) {
            for ( int e = bytes ; e >= 0 ; e-- ) {
                out.write( ENCODABET[ (int) (sum / FACTORS[e]) ] );
                sum %= FACTORS[e];
            }
        }
    }
}