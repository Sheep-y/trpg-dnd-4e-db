package db4e.exporter;

import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 *\ Export raw data as Json
 */
public class ExporterRawSql extends Exporter {

   private Writer writer;

   private static final ButtonType MYSQL = new ButtonType( "MySQL" );
   private static final ButtonType MSSQL = new ButtonType( "MS SQL" );
   private static final ButtonType POSTGRE = new ButtonType( "ANSI (Postgre)" );

   private char id_quote_start;
   private char id_quote_end;
   private char string_prefix;
   private String varchar; // max 303
   private String text; // max 127599

   @Override public synchronized void setState( File target, Consumer<String> stopChecker, ProgressState state ) {
      super.setState(target, stopChecker, state);
      ButtonType choice = new Alert( Alert.AlertType.CONFIRMATION, "Select database type:", MYSQL, MSSQL, POSTGRE, ButtonType.CANCEL ).showAndWait().orElse( ButtonType.CANCEL );
      if ( choice.equals( MYSQL ) ) {
         id_quote_start = id_quote_end = '`';
         string_prefix = ' ';
         varchar = " VARCHAR";
         text = " MEDIUMTEXT";
      } else if ( choice.equals( MSSQL ) ) {
         id_quote_start = '[';
         id_quote_end = ']';
         string_prefix = 'N';
         varchar = " NVARCHAR";
         text = " NTEXT";
      } else if ( choice.equals( POSTGRE ) ) {
         id_quote_start = id_quote_end = '"';
         string_prefix = ' ';
         varchar = " VARCHAR";
         text = " TEXT";
      } else //if ( choice.equals( ButtonType.CANCEL ) )
         throw new RuntimeException( "Cancelled" );
   }

   @Override protected void _preExport ( List<Category> categories ) throws IOException, InterruptedException {
      log.log( Level.CONFIG, "Export raw {1}Sql{2}: {0}", new Object[]{ target, id_quote_start, id_quote_end } );
      target.getParentFile().mkdirs();
      synchronized ( this ) {
         writer = openStream( target.toPath() );
         if ( id_quote_start == '`' || id_quote_start == '"' )
            writer.write( "SET NAMES 'UTF8';\n" );
      }
   }

   @Override protected void _export ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Building {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      int maxField = category.fields.length - 1;
      int[] maxLen = new int[ category.fields.length + 2 ];
      for ( Entry entry : category.entries ) {
         if ( entry.getUrl().length() > maxLen[0] ) maxLen[0] = entry.getUrl().length();
         if ( entry.getName().length() > maxLen[1] ) maxLen[1] = entry.getName().length();
         for ( int i = 0 ; i <= maxField ; i++ )
            if ( entry.getSimpleField( i ).length() > maxLen[i+2] )
               maxLen[i+2] = entry.getSimpleField( i ).length();
      }

      StringBuilder buffer = new StringBuilder( 3 * 1024 * 1024 );
      id( buffer.append( "\nDROP TABLE IF EXISTS " ), category.id ).append( ";\n" );
      id( buffer.append( "CREATE TABLE " ), category.id ).append( "(\n  " );
      id( buffer, "Url" ).append( varchar ).append( '(' ).append( maxLen[0] ).append( ") NOT NULL PRIMARY KEY,\n  " );
      id( buffer, "Name" ).append( varchar ).append( '(' ).append( maxLen[1] ).append( ") NOT NULL,\n  " );
      for ( int i = 0 ; i <= maxField ; i++ )
         id( buffer, category.fields[i] ).append( varchar ).append( '(' ).append( maxLen[i+2] ).append( ") NOT NULL,\n  " );
      id( buffer, "Content" ).append( text ).append( " NOT NULL \n   " );
      buffer.append( ") " );

      int rowCount = 0;
      for ( Entry entry : category.entries ) {
         if ( ! entry.hasContent() ) continue;
         if ( rowCount++ % 20 == 0 )
            id( backspace( buffer ).append( ";\nINSERT INTO "), category.id ).append( " VALUES " );
         buffer.append( "\n(" );
         txt( buffer, entry.getUrl() ).append( ',' );
         txt( buffer, entry.getName() ).append( ',' );
         for ( String field : entry.getSimpleFields() )
            txt( buffer, field ).append( ',' );
         txt( buffer, entry.getContent() );
         buffer.append( ")," );
      }

      if ( ! buffer.toString().endsWith( " VALUES " ) ) { // If any entry was written
         backspace( buffer ).append( ";\n" );
         synchronized ( this ) {
            writer.write( buffer.toString() );
         }
      }
      state.add( category.entries.size() );
   }

   @Override public synchronized void close() throws IOException {
      if ( writer == null ) return;
      writer.close();
      writer = null;
   }

   private StringBuilder id ( StringBuilder buffer, String id ) {
      return buffer.append( id_quote_start ).append( id ).append( id_quote_end );
   }

   private StringBuilder txt ( StringBuilder buffer, String text ) {
      return buffer.append( string_prefix ).append( '\'' ).append( text.replace( "'", "''" ) ).append( '\'' );
   }
}