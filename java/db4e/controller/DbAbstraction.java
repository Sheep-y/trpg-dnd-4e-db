package db4e.controller;

import db4e.Main;
import db4e.data.Category;
import db4e.data.Entry;
import db4e.data.EntryDownloaded;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.ObservableList;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import sheepy.util.JavaFX;

/**
 * Database abstraction.
 * Note that SqlJet does not support multi-thread.
 */
class DbAbstraction {

   private static final Logger log = Main.log;

   private volatile SqlJetDb db;

   void setDb ( SqlJetDb db, ObservableList<Category> categories, ProgressState state ) throws SqlJetException {
      this.db = db;
      ISqlJetTable tblConfig = db.getTable( "config" );

      // Check version
      log.fine( "Tables loaded. Checking data version." );
      int version = -1;
      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetCursor cursor = tblConfig.lookup( null, "version" );
         if ( ! cursor.eof() ) version = Integer.parseInt( cursor.getString( "value" ) );
         cursor.close();

         if ( version < 20160718 )
            throw new UnsupportedOperationException( "dnd4e database version (" + version + ") mismatch or not found." );
      } finally {
         db.commit();
      }
      log.log( Level.CONFIG, "Database version {0,number,#}, opened.", version );

      loadCategory( categories );
      loadEntryIndex( categories, state );
   }

   void createTables () throws SqlJetException {
      db.beginTransaction( SqlJetTransactionMode.WRITE );
      try {
         db.createTable( "CREATE TABLE 'config' ('key' TEXT PRIMARY KEY NOT NULL, 'value' TEXT NOT NULL);" );

         db.createTable( "CREATE TABLE 'category' ("+
            " 'id' TEXT PRIMARY KEY NOT NULL,"+
            " 'name' TEXT NOT NULL,"+
            " 'count' INTEGER NOT NULL,"+
            " 'fields' TEXT NOT NULL,"+
            " 'type' TEXT NOT NULL,"+
            " 'order' INTEGER NOT NULL);" );
         db.createIndex( "CREATE INDEX category_order_index ON category(order)" );

         db.createTable( "CREATE TABLE 'entry' ("
                 + " 'id' TEXT PRIMARY KEY NOT NULL,"
                 + " 'name' TEXT NOT NULL,"
                 + " 'category' TEXT NOT NULL,"
                 + " 'fields' TEXT NOT NULL,"
                 + " 'hasData' TINYINT NOT NULL,"
                 + " 'data' TEXT);" );
         db.createIndex( "CREATE INDEX entry_category_index ON entry(category, hasData)" );

         ISqlJetTable tblConfig = db.getTable( "config" );
         ISqlJetTable tblCategory = db.getTable( "category" );

         tblConfig.insert( "version", "20160718" );

         tblCategory.insert( "Race", "Race", 0, "DescriptionAttribute,Size,SourceBook", "PC", 100 );
         tblCategory.insert( "Background", "Background", 0, "Type,Campaign,Skills,SourceBook", "PC", 200 );
         tblCategory.insert( "Theme", "Theme", 0, "SourceBook", "PC", 300 );
         tblCategory.insert( "Class", "Class", 0, "RoleName,PowerSourceText,KeyAbilities,SourceBook", "PC", 400 );
         tblCategory.insert( "ParagonPath", "Paragon Path", 0, "Prerequisite,SourceBook", "PC", 500 );
         tblCategory.insert( "EpicDestiny", "Epic Destiny", 0, "Prerequisite,SourceBook", "PC", 600 );
         tblCategory.insert( "Feat", "Feat", 0, "TierName,SourceBook", "PC", 700 );
         tblCategory.insert( "Power", "Power", 0, "ClassName,Level,ActionType,SourceBook", "PC", 800 );
         tblCategory.insert( "Ritual", "Ritual", 0, "Level,ComponentCost,Price,KeySkillDescription,SourceBook", "PC", 900 );
         tblCategory.insert( "Companion", "Companion", 0, "Type,SourceBook", "PC", 1000 );
         tblCategory.insert( "Item", "Item", 0, "Category,Level,Cost,Rarity,SourceBook", "PC", 1100 );

         tblCategory.insert( "Monster", "Monster", 0, "Level,CombatRole,GroupRole,SourceBook", "DM", 1200 );
         tblCategory.insert( "Trap", "Trap", 0, "Type,GroupRole,Level,SourceBook", "DM", 1300 );
         tblCategory.insert( "Terrain", "Terrain", 0, "Type,SourceBook", "DM", 1400 );
         tblCategory.insert( "Poison", "Poison", 0, "Level,Cost,SourceBook", "DM", 1500 );
         tblCategory.insert( "Disease", "Disease", 0, "Level,SourceBook", "DM", 1600 );
         tblCategory.insert( "Deity", "Deity", 0, "Alignment,SourceBook", "DM", 1700 );
         tblCategory.insert( "Glossary", "Glossary", 0, "Category,Type,SourceBook", "DM", 1800 );

         db.commit();
      } finally {
         db.rollback();
      }
   }

   private void loadCategory ( ObservableList<Category> categories ) throws SqlJetException {
      log.fine( "Loading categories." );
      List<Category> list = new ArrayList<>();

      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetTable tblCategory = db.getTable( "category" );
         ISqlJetCursor cursor = tblCategory.order( "category_order_index" );
         if ( ! cursor.eof() ) { synchronized ( list ) {
            do {
               Category category = new Category(
                  cursor.getString( "id" ),
                  cursor.getString( "name" ),
                  parseCsvLine( cursor.getString( "fields" ) ) );
               category.total_entry.set( (int) cursor.getInteger( "count" ) );
               list.add( category );
            } while ( cursor.next() );
         } } else {
            throw new UnsupportedOperationException( "dnd4e database does not contains category." );
         }
         cursor.close();
         JavaFX.runNow( () -> { synchronized ( list ) {
            categories.clear();
            categories.addAll( list );
         } } );
         log.log( Level.FINE, "Loaded {0} categories.", list.size() );

      } finally {
         db.commit();
      }
   }

   private void loadEntryIndex ( List<Category> categories, ProgressState state ) throws SqlJetException {
      int downCount = 0;
      state.reset();

      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetTable tblEntry = db.getTable( "entry" );
         ISqlJetCursor cursor = tblEntry.open();
         final int total = (int) cursor.getRowCount();
         state.total = total;
         cursor.close();

         for ( Category category : categories ) synchronized( category ) {
            int countWithData = 0, size = category.total_entry.get();
            List<Entry> list = category.entries;
            list.clear();

            if ( category.total_entry.get() > 0 ) {
               cursor = tblEntry.lookup( "entry_category_index", category.id );
               if ( ! cursor.eof() ) do {
                  final EntryDownloaded entry = new EntryDownloaded( cursor.getString( "id" ), cursor.getString( "name" ) );
                  list.add( entry );
                  if ( cursor.getInteger( "hasData" ) != 0 ) {
                     entry.setContentDownloaded(true);
                     ++countWithData;
                  }
                  state.addOne();
               } while ( cursor.next() );
               cursor.close();
               if ( list.size() != size )
                  throw new IllegalStateException( category.name + " entry mismatch, expected " + size + ", read " + list.size() );
            }
            category.downloaded_entry.set( countWithData );
            downCount += countWithData;
         }
         state.update();

      } finally {
         db.commit();
      }
      state.set( downCount );
   }

   void loadEntityContent ( List<Category> categories, ProgressState state ) throws SqlJetException {
      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try { synchronized ( categories ) {
         state.total = categories.stream().mapToInt( e -> e.entries.size() ).sum();
         ISqlJetTable tblEntry = db.getTable( "entry" );
         for ( Category category : categories ) synchronized( category ) {
            log.log( Level.FINE, "Loading {0} content", category.id );
            for ( Entry entry : category.entries ) {
               if ( entry.getFields() == null || entry.getContent() == null ) {
                  ISqlJetCursor cursor = tblEntry.lookup( null, entry.getId() );
                  if ( cursor.eof() ) throw new IllegalStateException( "'" + entry.getName() + "' not in database" );
                  if ( entry.getFields()  == null ) entry.setFields( parseCsvLine( cursor.getString( "fields" ) ) );
                  if ( entry.getContent() == null ) entry.setContent( cursor.getString( "data" ) );
                  cursor.close();
               }
               state.addOne();
            }
         }
      } } finally {
         db.commit();
      }
   }

   void saveEntryList ( Category category, List<Entry> entries ) throws SqlJetException {
      int count = entries.size();
      db.beginTransaction( SqlJetTransactionMode.WRITE );
      try {
         ISqlJetTable tblCategory = db.getTable( "category" );
         ISqlJetTable tblEntry = db.getTable( "entry" );
         int i = 0;
         for ( Entry entry : entries ) {
            log.log( Level.FINER, "Saving {0} - {1}", new Object[]{ entry.getId(), entry.getName() } );
            ISqlJetCursor lookup = tblEntry.lookup( null, entry.getId() );
            // Table fields: id, name, category, fields, hasData, data
            String fields = buildCsvLine( entry.getFields() ).toString();
            if ( lookup.eof() ) {
               tblEntry.insert( entry.getId(), entry.getName(), category.id, fields, 0, null );
//            } else { // Shouldn't need to update.
//               lookup.update( entry.id, entry.name, category.id, fields );
            }
            lookup.close();
         }

         // Table fields: id, name, count, fields, type, order
         log.log( Level.FINE, "Updating {0} count", category.id );
         ISqlJetCursor owner = tblCategory.lookup( null, category.id );
         if ( owner.eof() )
            throw new IllegalStateException( "Category " + category.id + " not found in database." );
         owner.update( category.id, category.name, count );
         owner.close();
         category.entries.clear();
         category.entries.addAll( entries );
         category.total_entry.set( count );
         db.commit();

      } finally {
         db.rollback();
      }
   }

   private Map<String, Object> entryUpdateMap;

   void saveEntry ( Entry entry ) throws SqlJetException {
      if ( entryUpdateMap == null ) {
         entryUpdateMap = new HashMap<>( 2, 1f );
         entryUpdateMap.put( "hasData", 1 );
      }
      db.beginTransaction( SqlJetTransactionMode.WRITE );
      try {
         ISqlJetTable tblEntry = db.getTable( "entry" );
         ISqlJetCursor cursor = tblEntry.lookup( null, entry.getId() );
         if ( cursor.eof() ) throw new IllegalStateException( "'" + entry.getName() + "' not in database" );
         entryUpdateMap.put( "data", entry.getContent() );
         cursor.updateByFieldNames( entryUpdateMap );
         db.commit();
         entry.downloaded().setContentDownloaded(true);

      } finally {
         db.rollback();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utils
   /////////////////////////////////////////////////////////////////////////////

   private final String csvTokenPattern = "(?<=^|,)([^\"\\r\\n,]*|\"(?:\"\"|[^\"])*\")(?:,|$)";
   private final Matcher csvToken = Pattern.compile( csvTokenPattern ).matcher( "" );
   private final List<String> csvBuffer = new ArrayList<>();

   private synchronized String[] parseCsvLine ( CharSequence line ) {
      csvToken.reset( line );
      csvBuffer.clear();
      int pos = 0;
      while ( csvToken.find() ) {
         if ( csvToken.start() != pos )
            log.log( Level.WARNING, "CSV parse error: {0}", line );
         String token = csvToken.group( 1 );
         if ( token.length() >= 2 && token.charAt(0) == '"' && token.endsWith( "\"" ) )
            token = token.substring( 1, token.length()-1 ).replaceAll( "\"\"", "\"" );
         csvBuffer.add(token);
         pos = csvToken.end();
      }
      if ( pos != line.length() )
         log.log( Level.WARNING, "CSV parse error: {0}", line );
      return csvBuffer.toArray( new String[ csvBuffer.size() ] );
   }

   private final Matcher csvQuotable = Pattern.compile( "[\r\n,\"]" ).matcher( "" );

   private synchronized StringBuilder buildCsvLine ( String[] line ) {
      StringBuilder result = new StringBuilder(32);
      for ( String token : line ) {
         if ( csvQuotable.reset( token ).find() )
            result.append( '"' ).append( token.replaceAll( "\"", "\"\"" ) ).append( "\"," );
         else
            result.append( token ).append( ',' );
      }
      result.setLength( result.length() - 1 );
      return result;
   }
}