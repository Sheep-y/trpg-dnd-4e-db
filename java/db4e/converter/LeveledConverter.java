package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class LeveledConverter extends Converter {

   protected int LEVEL = -1;

   protected LeveledConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise () {
      super.initialise();
      LEVEL = Arrays.asList( category.fields ).indexOf( "Level" );
      // if ( LEVEL < 0 ) throw new IllegalStateException( "Level field not in " + category.name );
   }

   static final Map<Object, Float> levelMap = new HashMap<>();

   @Override protected void beforeSort () {
      super.beforeSort();
      if ( LEVEL < 0 ) return;
      synchronized ( levelMap ) { // Make sure that sortEntity has all the numbers in this category
         for ( Entry entry : category.entries ) {
            Object levelText = entry.getField( LEVEL );
            if ( levelText.getClass().isArray() )
               levelText = ( (Object[]) levelText )[1];
            String level = levelText.toString();
            if ( ! levelMap.containsKey( level ) ) {
               float lv = parseLevel( level );
               levelMap.put( level, lv );
               if ( lv < -10 ) warn( "Unknown level \"" + levelText + "\"" );
            }
         }
      }
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
      final String valueText = value.toString();
      switch ( valueText ) {
         case "-": // Rubble Topple, Brazier Topple, Donjon's Cave-in.
         case "":
            return -1f;
         case "Mundane":
            return 0f;
         case "Improvised":
            return 0.1f;
         case "Simple":
            return 0.2f;
         case "Military":
            return 0.3f;
         case "Superior":
            return 0.4f;
         case "Heroic":
            return 10.5f;
         case "Paragon":
            return 20.5f;
         case "Epic":
            return 30.5f;
         default:
            if ( valueText.startsWith( "Min" ) )
               return Integer.valueOf( valueText.substring( 5 ) ) / 10f;
            else if ( valueText.toLowerCase().startsWith( "vari" ) || valueText.toLowerCase().contains( "level" ) )
               // variable / Variable / Varies / (Level) / Party's Level
               return 40.5f;
            else
               return -1000f;
      }
   }

   protected int sortLevel ( Entry a, Entry b ) {
      if ( LEVEL >= 0 ) {
         Object aLv = a.getField( LEVEL );
         Object bLv = b.getField( LEVEL );
         if ( aLv.getClass().isArray() ) aLv = ( ( Object[] ) aLv )[1];
         if ( bLv.getClass().isArray() ) bLv = ( ( Object[] ) bLv )[1];
         float level = levelMap.get( aLv ) - levelMap.get( bLv );
         if ( level < 0 )
            return -1;
         else if ( level > 0 )
            return 1;
      }
      return 0;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int lv = sortLevel( a, b );
      return lv != 0 ? lv : super.sortEntity( a, b );
   }

   @Override protected void correctEntry () {
      switch ( category.id ) {
      case  "Poison":
         if ( find( "<p>Published in" ) ) {
            swap( "<p>Published in", "<p class=publishedIn>Published in" );
            fix( "formatting" );
         }

         // Convert from item to poison
         if ( entry.getFieldCount() == 5 ) {
            meta( meta( 1 ), "", meta( 4 ) );
            entry.setId( entry.getId().replace( "item", "poison0" ) );
            swap( "<h1 class=mihead>", "<h1 class=poison>" );
            fix( "recategorise" );
         }

         switch ( entry.getId() ) {
         case "poison19": // Granny's Grief
            swap( ">Published in .<", ">Published in Dungeon Magazine 211.<" );
            fix( "missing published" );
            break;
         case "poison03561": // Aboleth Slime Concentrate
         case "poison03562": // Gibbering Grind
         case "poison03563": // Grell Bile
         case "poison03564": // Umber Dust
         case "poison03565": // Heart of Mimic Powder
         case "poison03566": // Mind Flayer Tentacle Extract
            swap( " (Consumable)", "" );
            swap( "(Consumable, ", "(" );
            swap( " ✦ (", " ✦ Consumable (" );
            fix( "missing power frequency" );
         }
         break;

      case "Ritual":
         switch ( entry.getId() ) {

         case "ritual288": // Primal Grove
            swap( " grp to ", " gp to " );
            fix( "typo" );
            break;
         }
      }
   }
}