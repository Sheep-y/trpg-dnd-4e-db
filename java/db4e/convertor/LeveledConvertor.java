package db4e.convertor;

import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class LeveledConvertor extends Convertor {

   private int levelMetaIndex = -1;

   protected LeveledConvertor ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override public void convert ( ProgressState state ) {
      if ( category.meta == null )
         category.meta = category.fields;
      levelMetaIndex = Arrays.asList( category.meta ).indexOf( "Level" );
      if ( levelMetaIndex < 0 ) throw new IllegalStateException( "Level field not in " + category.name );
      super.convert( state );
   }

   static Map<String, String> levelText = new HashMap<>(); // Cache level text
   static Map<Object, Float> levelNumber = new HashMap<>();

   @Override protected void convertEntry ( Entry entry ) {
      super.convertEntry( entry );
      String level = entry.meta[ levelMetaIndex ].toString();
      if ( levelText.containsKey( level ) ) {
         entry.meta[ levelMetaIndex ] = level = levelText.get( level );
      } else {
         if ( level.endsWith( "Minion" ) && category.id.equals( "Trap" ) ) {
            // 7 traps in Dungeon 214-215 has level like "8 Minion" and no group role.
            entry.meta[ Arrays.asList( category.meta ).indexOf( "GroupRole" ) ] = "Minion";
            entry.meta[ levelMetaIndex ] = level = level.substring( 0, level.length() - "Minion".length() - 1 );
         }
         levelText.put( level, level );
         levelNumber.put( level, parseLevel( level ) );
      }
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      float level = levelNumber.get( a.meta[ levelMetaIndex ] ) - levelNumber.get( b.meta[ levelMetaIndex ] );
      if ( level < 0 )
         return -1;
      else if ( level > 0 )
         return 1;
      else
         return a.name.compareTo( b.name );
   }

   private float parseLevel ( Object value ) {
      if ( value == null ) return -1;
      String level = value.toString();
      try {
         return Integer.valueOf( level );
      } catch ( NumberFormatException ex1 ) {
         if ( level.endsWith( "+" ) ) level = level.substring( 0, level.length() - 1 );
         try {
            return Integer.valueOf( level );
         } catch ( NumberFormatException ex2 ) {
         }
      }
      switch ( value.toString() ) {
         case "-": // Rubble Topple, Brazier Topple, Donjon's Cave-in.
         case "":
            return 0f;
         case "Heroic":
            return 10.5f;
         case "Paragon":
            return 20.5f;
         case "Epic":
            return 30.5f;
         default:
            // variable / Variable / Varies / (Level) / Party's Level
            return 40.5f;
      }
   }

}
