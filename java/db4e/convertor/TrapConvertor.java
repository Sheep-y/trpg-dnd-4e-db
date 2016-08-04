package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;

public class TrapConvertor extends LeveledConvertor {

   public TrapConvertor ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected String correctEntry (Entry entry) {
      if ( entry.meta.length == 4 ) { // Trap
         // 7 traps in Dungeon 214-215 has level like "8 Minion" and no group role.
         String level = entry.meta[ LEVEL ].toString();
         if ( level.endsWith( "Minion" ) ) {
            entry.meta[ Arrays.asList( category.meta ).indexOf( "GroupRole" ) ] = "Minion";
            entry.meta[ LEVEL ] = level.substring( 0, level.length() - " Minion".length() );
            return "formatting";
         }
      } else {
         // Terrain; change meta to fit into Trap
         entry.meta = new Object[]{ "Terrain", entry.fields[ 0 ], entry.fields[ 1 ] };
      }
      return null;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ 0 ].toString().compareTo( b.meta[ 0 ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }
}