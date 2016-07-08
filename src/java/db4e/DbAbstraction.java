package db4e;

import java.util.zip.DataFormatException;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

class DbAbstraction {

   private SqlJetDb db; 
      private ISqlJetTable tblConfig; 
      private ISqlJetTable tblCategory;
      private ISqlJetTable tblEntry;

   synchronized int setDb( SqlJetDb db ) throws SqlJetException, DataFormatException {
      this.db = db;
      tblConfig = db.getTable( "config" );
      tblCategory = db.getTable( "category" );
      tblEntry = db.getTable( "entry" );

      // Check version
      int version = -1;
      db.beginTransaction( SqlJetTransactionMode.READ_ONLY );
      try {
         ISqlJetCursor cursor = tblConfig.lookup( null, "version" );
         if ( ! cursor.eof() ) version = Integer.parseInt( cursor.getString( "value" ) );
         cursor.close();
         
         if ( version < 20160706) throw new DataFormatException( "dnd4e database version (" + version + ") mismatch or not found." );
      } finally {
         db.commit();
      }
      return version;
   }

   synchronized void createTables() throws SqlJetException {
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

         tblCategory.insert( "Race", "Race", -1, "Size, DescriptionAttribute, SourceBook", "PC", 10 );
         tblCategory.insert( "Background", "Background", -1, "Type, Campaign, Skills, SourceBook", "PC", 20 );
         tblCategory.insert( "Theme", "Theme", -1, "SourceBook", "PC", 30 );
         tblCategory.insert( "Class", "Class", -1, "PowerSourceText, RoleName, KeyAbilities, SourceBook", "PC", 40 );
         tblCategory.insert( "ParagonPath", "Paragon Path", -1, "Prerequisite, SourceBook", "PC", 50 );
         tblCategory.insert( "EpicDestiny", "Epic Destiny", -1, "Prerequisite, SourceBook", "PC", 60 );
         tblCategory.insert( "Feat", "Feat", -1, "SourceBook, TierName, TierSort", "PC", 70 );
         tblCategory.insert( "Power", "Power", -1, "Level, ActionType, SourceBook, ClassName", "PC", 80 );
         tblCategory.insert( "Ritual", "Ritual", -1, "Level, ComponentCost, Price, KeySkillDescription, SourceBook", "PC", 90 );
         tblCategory.insert( "Companion", "Companion", -1, "Type, SourceBook", "PC", 100 );
         tblCategory.insert( "Item", "Item", -1, "Cost, Level, Rarity, Category, SourceBook, LevelSort, CostSort", "PC", 110 );

      db.commit();
   }


}
