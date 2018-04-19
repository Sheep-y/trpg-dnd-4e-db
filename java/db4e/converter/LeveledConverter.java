package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

   private Matcher regxFlavor;
   protected void compileFlavorBr () {
      regxFlavor = Pattern.compile( "(?<=</h1>)<p class=flavor>([^<]*|</?(?!p)[^>]+/?>)+</p>" ).matcher( "" );
   }

   /* Removes <br> from flavor text, called manually by Power and Trap.  Other tags has not been found.  Each entries that need to be fixed only has one flavor text. */
   protected void stripFlavorBr () {
      if ( ! find( regxFlavor ) ) return;
      String matched = regxFlavor.group();
      if ( ! matched.contains( "<br>" ) ) return;
      swap( matched, matched.replaceAll( "<br>", " " ), "formatting" );
   }
}