package sheepy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Resource {

   /**
    * Open a file in the file system or in the jar.
    * File system is tried first, if the file does not exist then try class.getResourceAsStream.
    * 
    * @param file File to open.  Path is relative to current path / jar root of this class.
    * @return Input stream of the resource.  Return null if the file cannot be foung.
    */
   public static InputStream getStream ( String file ) {
      try {
         File f = new File( file );
         if ( f.exists() && f.isFile() && f.canRead() ) {
            return new FileInputStream( f );
         } else {
            file = file.replace( '\\', '/' );
            if ( ! file.startsWith( "/" ) ) file = '/' + file;
            return Resource.class.getResourceAsStream( file );
         }
      } catch ( FileNotFoundException ex ) {
         return null;
      }
   }

   /**
    * Read a file - in the file system or in the jar - as a string.
    * @param file File to read.  Path is relative to current path / jar root.
    * @return File content as utf-8 string.
    * @throws IOException Error during file / jar read.
    */
   public static String getText ( String file ) throws IOException {
      InputStream is = getStream( file );
      if ( is == null ) throw new FileNotFoundException( "Resource not found: " + file );
      return getText( is );
   }

   /**
    * Read the whole utf-8 input stream as a string.
    *
    * @param is Input stream
    * @return Stream content as string
    * @throws IOException Error during stream read.
    */
   public static String getText ( InputStream is ) throws IOException {
      try {
         return new Scanner( is, "UTF-8" ).useDelimiter("\\A").next();
      } catch ( NoSuchElementException ex ) {
         return "";
      } finally {
         is.close();
      }
   }
}