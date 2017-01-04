package db4e.exporter;

import db4e.Main;
import db4e.controller.Controller;
import db4e.controller.ProgressState;
import db4e.data.Category;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import sheepy.util.ResourceUtils;

/**
 * Base exporter class that provides export interface and support functions.
 */
public abstract class Exporter {

   public static AtomicBoolean stop = new AtomicBoolean();
   protected static final Logger log = Main.log;

   private Consumer<String> stopChecker;
   protected File target;
   protected ProgressState state;

   public void setState ( File target, Consumer<String> stopChecker, ProgressState state ) {
      this.stopChecker = stopChecker;
      this.target = target;
      this.state = state;
   }

   public abstract void preExport ( List<Category> categories ) throws IOException;
   public abstract Controller.RunExcept export ( Category category ) throws IOException;
   public void postExport ( List<Category> categories ) throws IOException {};

   protected void checkStop ( String status ) {
      stopChecker.accept( status );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   protected final OutputStreamWriter openStream ( String path ) throws FileNotFoundException {
      return new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( path, false ) ), StandardCharsets.UTF_8 );
   }

   protected final StringBuilder backspace ( StringBuilder buf ) {
      buf.setLength( buf.length() - 1 );
      return buf;
   }

   protected final void write ( Writer writer, String buf ) throws IOException {
      writer.write( buf );
   }

   protected final void write ( CharSequence postfix, Writer writer, StringBuilder buf ) throws IOException {
      if ( ! ( postfix.charAt( 0 ) == ',' ) )
         backspace( buf ); // Remove last comma if postfix does not start with comma
      buf.append( postfix );
      writer.write( buf.toString() );
      buf.setLength( 0 );
   }

   protected final StringBuilder str ( StringBuilder buf, String txt ) {
      return buf.append( '"' ).append( js( txt ) ).append( '"' );
   }

   protected final String js ( String in ) {
      return in.replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
   }

   protected final StringBuilder csv ( StringBuilder buf, String in ) {
      if ( in.contains( "\"" ) || in.contains( "\n" ) )
         return buf.append( '"' ).append( in.replace( "\"", "\"\"" ) ).append( '"' );
      return buf.append( in );
   }

   void copyRes ( String target, String source ) throws IOException {
      try ( OutputStream out = new FileOutputStream( target, false );
            InputStream in = ResourceUtils.getStream( source );
              ) {
         byte[] buffer =  new byte[ 32768 ];
         for ( int length ; (length = in.read( buffer ) ) != -1; )
            out.write( buffer, 0, length );
      }
   }
}