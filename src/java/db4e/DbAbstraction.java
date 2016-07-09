package db4e;

import db4e.data.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

   synchronized int setDb ( SqlJetDb db, ObservableList<Category> categories ) throws SqlJetException {
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

         if ( version < 20160706 )
            throw new UnsupportedOperationException( "dnd4e database version (" + version + ") mismatch or not found." );
      } finally {
         db.commit();
      }

      loadCategory( categories );
      return version;
   }

   synchronized void createTables () throws SqlJetException {
      db.beginTransaction( SqlJetTransactionMode.WRITE );
         db.createTable( "CREATE TABLE 'config' ('key' TEXT PRIMARY KEY NOT NULL, 'value' TEXT);" );

         db.createTable( "CREATE TABLE 'category' ("+
            " 'id' TEXT PRIMARY KEY NOT NULL,"+
            " 'name' TEXT NOT NULL,"+
            " 'count' INTEGER NOT NULL,"+
            " 'fields' TEXT NOT NULL,"+
            " 'type' TEXT NOT NULL,"+
            " 'order' INTEGER NOT NULL);" );
         db.createIndex( "CREATE INDEX category_order_index ON category(order)" );

         db.createTable( "CREATE TABLE 'entry' ('id' TEXT PRIMARY KEY NOT NULL, 'name' TEXT, 'fields' TEXT, 'data' TEXT);" );

         tblConfig = db.getTable( "config" );
         tblCategory = db.getTable( "category" );
         tblEntry = db.getTable( "entry" );

         tblConfig.insert( "version", "20160706" );

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

   private void loadCategory ( ObservableList<Category> categories ) throws SqlJetException {
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
                  cursor.getString( "fields" ).split( "," ) );
               category.total_entry.set( (int) cursor.getInteger( "count" ) );
               list.add( category );
            } while ( cursor.next() );
         } else {
            throw new UnsupportedOperationException( "dnd4e database does not contains category." );
         }
         cursor.close();
      } finally {
         db.commit();
      }

      log.log( Level.FINE, "Loaded {0} categories.", list.size() );
      categories.clear();
      categories.addAll( list );
   }


}
