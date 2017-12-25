package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;

public class TrapConverter extends LeveledConverter {

   private final int TYPE = 0;
   private final int ROLE = 1;

   public TrapConverter ( Category category ) {
      super( category );
   }

   @Override protected void correctEntry () {
      if ( entry.getFieldCount() == 4 ) { // Trap

         switch ( entry.getId() ) {
            case "trap29": case "trap71": case "trap107": // Spider Cactus, Rock Flingers, Psychic Sinkhole
               stripFlavorBr();
               break;

            case "trap417" : // Elemental Transformation Field
               meta( ROLE, "Elite" );
               swap( "Level 18 Lurker", "Level 18 Elite Lurker" );
               fix( "consistency" );
               break;

            case "trap1019" : // Rubble Topple
               swap( "Singe-Use", "Single-Use" );
               meta( TYPE, "Terrain" );
               meta( ROLE, "Single-Use" );
               fix( "typo" );
               break;

            case "trap1101" : // Glyph of Warding
               entry.setName( "Glyph of Warding" );
               swap( "Elite Glyph of Warding", "Glyph of Warding" );
               swap( "Level 7 Trap", "Level 7 Elite Trap" );
               meta( ROLE, "Elite" );
               fix( "consistency" );
               break;
         }

         String type = meta( TYPE );
         String level = meta( LEVEL );
         if ( type.startsWith( "Minion " ) || type.startsWith( "Elite " ) || type.startsWith( "Solo " ) || type.startsWith( "Single-Use ") ) {
            // 33 traps / hazards has mixed type and role. 3 terrain can also be split this way.
            String[] roles = type.split( " ", 2 );
            meta( ROLE, roles[ 0 ] );
            meta( TYPE, roles[ 1 ] );
            fix( "wrong meta" );

         } else if ( level.endsWith( "Minion" ) ) {
            // 7 traps in Dungeon 214-215 has level like "8 Minion" and no group role.
            meta( ROLE, "Minion" );
            meta( LEVEL, level.substring( 0, level.length() - " Minion".length() ) );
            fix( "wrong meta" );
         }

         if ( entry.getName().endsWith( " (Elite)" ) ) {
            String from = entry.getName();
            String to = from.replace( " (Elite)", "" );
            entry.setName( to );
            swap( from, to );
            fix( "consistency" ); // All entries are already elite (correct meta), except trap417
         }

      } else {
         // Terrain; change meta to fit into Trap
         meta( "Terrain", meta( 0 ), "", meta( 1 ) );
      }
      super.correctEntry();
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( TYPE ).compareTo( b.getSimpleField( TYPE ) );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }
}