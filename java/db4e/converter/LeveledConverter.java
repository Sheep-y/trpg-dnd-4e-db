package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

class LeveledConverter extends Converter {

   protected int LEVEL = -1;

   protected LeveledConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise () {
      super.initialise();
      LEVEL = Arrays.asList( category.meta ).indexOf( "Level" );
      // if ( LEVEL < 0 ) throw new IllegalStateException( "Level field not in " + category.name );
   }

   static final Map<Object, Float> levelMap = new HashMap<>();

   @Override protected void beforeSort () {
      super.beforeSort();
      if ( LEVEL < 0 ) return;
      synchronized ( levelMap ) { // Make sure that sortEntity has all the numbers in this category
         for ( Entry entry : category.entries ) {
            Object levelText = entry.meta[ LEVEL ];
            if ( levelText.getClass().isArray() )
               levelText = ( (Object[]) levelText )[1];
            String level = levelText.toString();
            if ( ! levelMap.containsKey( level ) ) {
               float lv = parseLevel( level );
               levelMap.put( level, lv );
               if ( lv < -10 ) log.log( Level.WARNING, "Unknown level \"{0}\": {1} {2}", new Object[]{ levelText, entry.shortid, entry.name } );
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

   @Override protected int sortEntity ( Entry a, Entry b ) {
      if ( LEVEL >= 0 ) {
         Object aLv = a.meta[ LEVEL ];
         Object bLv = b.meta[ LEVEL ];
         if ( aLv.getClass().isArray() ) aLv = ( ( Object[] ) aLv )[1];
         if ( bLv.getClass().isArray() ) bLv = ( ( Object[] ) bLv )[1];
         float level = levelMap.get( aLv ) - levelMap.get( bLv );
         if ( level < 0 )
            return -1;
         else if ( level > 0 )
            return 1;
      }
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry ( Entry entry ) {
      switch ( category.id ) {
      case  "Poison":
         if ( entry.data.contains( "<p>Published in" ) ) {
            entry.data = entry.data.replace( "<p>Published in", "<p class=publishedIn>Published in" );
            corrections.add( "formatting" );
         }

         switch ( entry.shortid ) {
         case "poison19": // Granny's Grief
            entry.data = entry.data.replace( ">Published in .<", ">Published in Dungeon Magazine 211.<" );
            corrections.add( "missing published" );
            break;
         case "item3561": // Aboleth Slime Concentrate
         case "item3562": // Gibbering Grind
         case "item3563": // Grell Bile
         case "item3564": // Umber Dust
         case "item3565": // Heart of Mimic Powder
         case "item3566": // Mind Flayer Tentacle Extract
            entry.data = entry.data.replace( " (Consumable)", "" ).replace( "(Consumable, ", "(" );
            entry.data = entry.data.replace( " ✦ (", " ✦ Consumable (" );
            corrections.add( "missing power frequency" );
         }

         // Convert from item to poison
         if ( entry.meta.length == 5 ) {
            entry.meta = new Object[]{ entry.meta[1], "", entry.meta[4] };
            entry.shortid = entry.shortid.replace( "item", "poison0" );
            entry.data = entry.data.replace( "<h1 class=mihead>", "<h1 class=poison>" );
            corrections.add( "recategorise" );
         }
         break;

      case "Monster":
         switch ( entry.shortid ) {

         case "monster2248": // Cambion Stalwart
            entry.data = entry.data.replace( "bit points", "hit points" );
            corrections.add( "typo" );
            break;

         case "monster3222": // Veln
         case "monster3931": // Demon Furor
            entry.data = entry.data.replace( "basic melee or basic ranged attack", "melee or ranged basic attack" );
            corrections.add( "fix basic attack" );
            break;

         default:
            if ( entry.data.contains( "basic melee attack") ) {
               entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
               corrections.add( "fix basic attack" );
            }
         }
      case "Ritual":
         switch ( entry.shortid ) {

         case "ritual288": // Primal Grove
            entry.data = entry.data.replace( " grp to ", " gp to " );
            corrections.add( "typo" );
            break;
         }
      }
   }
}