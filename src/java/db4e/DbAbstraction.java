package db4e;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

class DbAbstraction {

   private static final Logger log = Main.log;

   private SqlJetDb db;
      private ISqlJetTable tblConfig;
      private ISqlJetTable tblCategory;
      private ISqlJetTable tblEntry;

   synchronized int setDb ( SqlJetDb db, ObservableList<Category> categories, Consumer<String> statusUpdate ) throws SqlJetException {
      this.db = db;
      tblConfig = db.getTable( "config" );
      tblCategory = db.getTable( "category" );
      tblEntry = db.getTable( "entry" );

      // Check version
      log.fine( "Tables loaded. Checking data version." );
      int version = -1;
      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetCursor cursor = tblConfig.lookup( null, "version" );
         if ( ! cursor.eof() ) version = Integer.parseInt( cursor.getString( "value" ) );
         cursor.close();

         if ( version < 20160710 )
            throw new UnsupportedOperationException( "dnd4e database version (" + version + ") mismatch or not found." );
      } finally {
         db.commit();
      }

      loadCategory( categories );
      loadEntryIndex( categories, statusUpdate );
      return version;
   }

   synchronized void createTables () throws SqlJetException {
      db.beginTransaction( SqlJetTransactionMode.WRITE );
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

         tblConfig = db.getTable( "config" );
         tblCategory = db.getTable( "category" );
         tblEntry = db.getTable( "entry" );

         tblConfig.insert( "version", "20160710" );

         tblCategory.insert( "Race", "Race", 0, "Size,DescriptionAttribute,SourceBook", "PC", 100 );
         tblCategory.insert( "Background", "Background", 0, "Type,Campaign,Skills,SourceBook", "PC", 200 );
         tblCategory.insert( "Theme", "Theme", 0, "SourceBook", "PC", 300 );
         tblCategory.insert( "Class", "Class", 0, "PowerSourceText,RoleName,KeyAbilities,SourceBook", "PC", 400 );
         tblCategory.insert( "ParagonPath", "Paragon Path", 0, "Prerequisite,SourceBook", "PC", 500 );
         tblCategory.insert( "EpicDestiny", "Epic Destiny", 0, "Prerequisite,SourceBook", "PC", 600 );
         tblCategory.insert( "Feat", "Feat", 0, "SourceBook,TierName,TierSort", "PC", 700 );
         tblCategory.insert( "Power", "Power", 0, "Level,ActionType,SourceBook,ClassName", "PC", 800 );
         tblCategory.insert( "Ritual", "Ritual", 0, "Level,ComponentCost,Price,KeySkillDescription,SourceBook", "PC", 900 );
         tblCategory.insert( "Companion", "Companion", 0, "Type,SourceBook", "PC", 1000 );
         tblCategory.insert( "Item", "Item", 0, "Cost,Level,Rarity,Category,SourceBook,LevelSort,CostSort", "PC", 1100 );

         tblCategory.insert( "Monster", "Monster", 0, "Level,GroupRole,CombatRole,SourceBook", "DM", 1200 );
         tblCategory.insert( "Trap", "Trap", 0, "GroupRole,Type,Level,SourceBook", "DM", 1300 );
         tblCategory.insert( "Terrain", "Terrain", 0, "Type,SourceBook", "DM", 1400 );
         tblCategory.insert( "Poison", "Poison", 0, "Level,Cost,SourceBook", "DM", 1500 );
         tblCategory.insert( "Disease", "Disease", 0, "Level,SourceBook", "DM", 1600 );
         tblCategory.insert( "Deity", "Deity", 0, "Alignment,SourceBook", "DM", 1700 );
         tblCategory.insert( "Glossary", "Glossary", 0, "Category,Type,SourceBook", "DM", 1800 );

      db.commit();
   }

   private synchronized void loadCategory ( ObservableList<Category> categories ) throws SqlJetException {
      log.fine( "Loading categories." );
      List<Category> list = new ArrayList<>();

      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetCursor cursor = tblCategory.order( "category_order_index" );
         if ( ! cursor.eof() ) {
            do {
               Category category = new Category(
                  cursor.getString( "id" ),
                  cursor.getString( "name" ),
                  cursor.getString( "type" ),
                  parseCsvLine( cursor.getString( "fields" ) ) );
               category.total_entry.set( (int) cursor.getInteger( "count" ) );
               list.add( category );
            } while ( cursor.next() );
         } else {
            throw new UnsupportedOperationException( "dnd4e database does not contains category." );
         }
         cursor.close();
         categories.clear();
         categories.addAll( list );
         log.log( Level.FINE, "Loaded {0} categories.", list.size() );

      } finally {
         db.commit();
      }

      // TODO: Backup good db.
   }

   synchronized void loadEntryIndex ( List<Category> categories, Consumer<String> statusUpdate ) throws SqlJetException {
      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetCursor cursor = tblEntry.open();
         final int total = (int) cursor.getRowCount();
         int count = 0;
         cursor.close();

         for ( Category category : categories) {
            int countWithData = 0;
            List<Entry> list = category.entries;
            list.clear();

            cursor = tblEntry.lookup( "entry_category_index", category.id );
            if ( ! cursor.eof() ) do {
               list.add( new Entry( cursor.getString( "id" ), cursor.getString( "name" ) ) );
               if ( cursor.getInteger( "hasData" ) != 0 )
                  ++countWithData;
               if ( ++count % 2048 == 0 )
                  statusUpdate.accept( count + "/" + total );
            } while ( cursor.next() );
            cursor.close();

            if ( list.size() != category.total_entry.get() ) {
               log.log( Level.SEVERE, "{0} entry mismatch, expected {1}, read {2}", new Object[]{ category.id, category.total_entry.get(), list.size() });
               category.total_entry.set( list.size() );
            }
            category.downloaded_entry.set( countWithData );
         }
      } finally {
         db.commit();
      }
   }

   synchronized void saveEntryList ( Category category, List<Entry> entries ) throws SqlJetException {
      int count = entries.size();
      db.beginTransaction( SqlJetTransactionMode.WRITE );
      try {
         int i = 0;
         for ( Entry entry : entries ) {
            log.log( Level.FINER, "Saving {0} - {1}", new Object[]{ entry.id, entry.name } );
            ISqlJetCursor lookup = tblEntry.lookup( null, entry.id );
            // Table fields: id, name, category, fields, hasData, data
            String fields = buildCsvLine( entry.fields ).toString();
            if ( lookup.eof() ) {
               tblEntry.insert( entry.id, entry.name, category.id, fields, 0, null );
//            } else { // Shouldn't need to update.
//               lookup.update( entry.id, entry.name, category.id, fields );
            }
            lookup.close();
         }

         // Table fields: id, name, count, fields, type, order
         log.log( Level.FINE, "Updating {0} count", category.id );
         ISqlJetCursor owner = tblCategory.lookup( null, category.id );
         if ( owner.eof() )
            throw new AssertionError( "Category " + category.id + " not found in database." );
         owner.update( category.id, category.name, count );
         owner.close();
         category.entries.clear();
         category.entries.addAll( entries );
         category.total_entry.set( count );

      } catch ( Exception e ) {
         db.rollback();

      } finally {
         db.commit();
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

   private final Matcher csvQuotable = Pattern.compile( "[\r\n,]" ).matcher( "" );

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