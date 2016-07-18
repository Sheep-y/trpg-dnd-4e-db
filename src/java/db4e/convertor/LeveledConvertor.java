package db4e.convertor;

import db4e.controller.ProgressState;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;

class LeveledConvertor extends Convertor {

   private int levelMetaIndex;

   protected LeveledConvertor ( Category category, boolean debug ) {
      super( category, debug );
   }

   public void convert ( ProgressState state ) {
      super.convert( state );
      levelMetaIndex = Arrays.asList( category.meta ).indexOf( "Level" );
      if ( levelMetaIndex < 0 ) throw new IllegalStateException( "Level field not in " + category.name );
   }

   protected void convertEntry ( Entry entry ) {
      super.convertEntry( entry );
      String level = entry.meta[ levelMetaIndex ].toString();
      try {
         entry.meta[ levelMetaIndex ] = Integer.parseInt( level );
      } catch ( NumberFormatException ex ) {
      }
   }

   protected int sortEntity ( Entry a, Entry b ) {
      float level = parseLevel( a.meta[ levelMetaIndex ] ) - parseLevel( b.meta[ levelMetaIndex ] );
      if ( level < 0 )
         return -1;
      else if ( level > 0 )
         return 1;
      else
         return a.name.compareTo( b.name );
   }

   private float parseLevel ( Object value ) {
      if ( value == null ) return -1;
      if ( value instanceof Number )
         return ( (Number) value ).floatValue();
      switch ( value.toString() ) {
         case "":
            return 0f;
         case "Heroic":
            return 10.5f;
         case "Paragon":
            return 20.5f;
         case "Epic":
            return 30.5f;
         default:
            System.out.println( value );
            return -1;
      }
   }

}
