package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Set;

public class MonsterConverter extends CreatureConverter {

   public MonsterConverter(Category category) {
      super( category );
      SIZE = 3;
      TYPE = 4;
      KeywordBlackList.add( "female" ); // monster115831 (x3)
      KeywordBlackList.add( "or" ); // monster1466 (x1)
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "Level", "CombatRole", "GroupRole", "Size", "CreatureType", "SourceBook" };
      super.initialise();
   }

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( fields[0], fields[1], fields[2], "", "", fields[3] );
      super.convertEntry();
   }

   @Override protected void correctEntry () {
      switch ( entry.getId() ) {
         case "monster2248": // Cambion Stalwart
            swap( "bit points", "hit points" );
            fix( "typo" );
            break;

         case "monster3222": // Veln
         case "monster3931": // Demon Furor
            swap( "basic melee or basic ranged attack", "melee or ranged basic attack" );
            fix( "fix basic attack" );
            break;

         case "monster3717": // Blistered Soul
            swap( "<span class=type> </span>", "<span class=type>Medium aberrant humanold</span>" );
            fix( "missing content" );
            // Fallthrough to fix basic attack

         default:
            if ( find( "basic melee attack") ) {
               swap( "basic melee attack", "melee basic attack" );
               fix( "fix basic attack" );
            }
      }
      if ( ! findSizeAndTypes() )
         warn( "Creature type not found" );
      super.correctEntry();
   }

   @Override protected Set<String> getLookupName( Entry entry, Set<String> list ) {
      if ( entry.getName().contains( "," ) ) // Named monsters like "Ninaran, Elf Archer"
         list.add( entry.getName().split( ",", 2 )[0] );
      return super.getLookupName( entry, list );
   }
}