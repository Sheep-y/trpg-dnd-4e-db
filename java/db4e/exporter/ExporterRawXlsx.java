package db4e.exporter;

import db4e.controller.Controller;
import db4e.data.Category;
import db4e.data.Entry;
import static db4e.exporter.Exporter.stop;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.ResourceUtils;

/**
 *\ Export raw data as XLSX
 */
public class ExporterRawXlsx extends Exporter {

   private FileSystem fs;

   @Override public void preExport ( List<Category> categories ) throws IOException {
      log.log( Level.CONFIG, "Export raw XLSX: {0}", target );
      StringBuilder buffer = new StringBuilder( 65535 );

      target.getParentFile().mkdirs();
      try ( InputStream reader = ResourceUtils.getStream( "res/xlsx.zip" ) ) {
         Files.copy( reader, target.toPath(), StandardCopyOption.REPLACE_EXISTING );
      }
      synchronized ( this ) {
         fs = FileSystems.newFileSystem( URI.create( "jar:" + target.toURI() ) , new HashMap<>() );
      }

      // Sheet list
      int id = 1;
      buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
              + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
              + "<fileVersion appName=\"xl\" lastEdited=\"1\" lowestEdited=\"1\"/>"
              + "<bookViews><workbookView xWindow=\"0\" yWindow=\"0\" windowWidth=\"0\" windowHeight=\"0\"/></bookViews><sheets>" );
      for ( Category category : categories )
         buffer.append( "<sheet name=\"" ).append( category.id ).append( "\" sheetId=\"" ).append( id++ ).append( "\" r:id=\"rId" ).append( category.id ).append( "\"/>" );
      buffer.append( "</sheets></workbook>" );
      try ( Writer writer = openStream( fs.getPath( "xl/workbook.xml" ) ) ) {
         writer.write( buffer.toString() );
      }

      // Sheet file location
      buffer.setLength( 0 );
      buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"theme/theme1.xml\"/>"
              + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>"
              + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" );
      for ( Category category : categories )
         buffer.append( "<Relationship Id=\"rId" ).append( category.id ).append( "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/" ).append( category.id ).append( ".xml\"/>" );
      buffer.append( "</Relationships>" );
      try ( Writer writer = openStream( fs.getPath( "xl/_rels/workbook.xml.rels" ) ) ) {
         writer.write( buffer.toString() );
      }

      state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
   }

   @Override public Controller.RunExcept export ( Category category ) throws IOException {
      return () -> { synchronized( category ) {
         writeCategory( category );
      } };
   }

   protected void writeCategory ( Category category ) throws IOException, InterruptedException {
      if ( stop.get() ) throw new InterruptedException();
      log.log( Level.FINE, "Writing {0} in thread {1}", new Object[]{ category.id, Thread.currentThread() });

      StringBuilder buffer = new StringBuilder( 65535 );
      buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
         "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\">"
              // Freeze top row
              + "<sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/><selection pane=\"bottomLeft\" activeCell=\"A2\" sqref=\"A2\"/></sheetView></sheetViews>"
              + "<sheetData>" );

      buffer.append( "<row>" );
      cell( buffer, "Url" );
      cell( buffer, "Name" );
      for ( String field : category.fields )
         buffer.append( "<c t=\"s\"><v>").append( field ).append( "</v></c>" );
      cell( buffer, "Content" );
      buffer.append( "</row>" );

      for ( Entry entry : category.entries ) {
         if ( ! entry.contentDownloaded ) continue;
         buffer.append( "<row>" );
         cell( buffer, "http://www.wizards.com/dndinsider/compendium/" + entry.id );
         cell( buffer, entry.name );
         for ( String field : entry.fields )
            cell( buffer, field );
         cell( buffer, entry.content );
         buffer.append( "</row>" );
      }
      buffer.append( "</sheetData></worksheet>" );

      if ( stop.get() ) throw new InterruptedException();

      try ( Writer writer = openStream( fs.getPath( "xl/worksheets/" + category.id + ".xml" ) ) ) {
         writer.write( buffer.toString() );
      }
      state.add( category.entries.size() );
   }

   @Override public void postExport( List<Category> categories ) throws IOException {
      fs.close();
   }

   private final Matcher regxNumber = Pattern.compile( "\\d+" ).matcher( "" );

   private StringBuilder cell ( StringBuilder buffer, String text ) throws InterruptedException {
      try {
         if ( text.isEmpty() )
            return buffer.append( "<c></c>" );
         else if ( regxNumber.reset( text ).matches() )
            return buffer.append( "<c><v>" ).append( text ).append( "</v></c>" );
         else
            return buffer.append( "<c t=\"s\"><v><![CDATA[").append( text ).append( "]]></v></c>" );
      } catch ( StringIndexOutOfBoundsException e1 ) {
         Thread.sleep( 20 ); // Random matcher bug
         return cell( buffer, text );
         /*
         log.log( Level.WARNING, "Error from thread {0} {1}", new Object[]{ Thread.currentThread(), this } );
         try {
            Integer.parseInt( text );
            return buffer.append( "<c><v>" ).append( text ).append( "</v></c>" );
         } catch ( NumberFormatException e2 ) {
            return buffer.append( "<c t=\"s\"><v><![CDATA[").append( text ).append( "]]></v></c>" );
         }*/
      }
   }

}