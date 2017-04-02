package db4e.converter;

import db4e.data.Category;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class MonsterConverter extends LeveledConverter {

   private static final int SIZE = 3;
   private static final int TYPE = 4;

   private final Set<String> AllSizes = new HashSet<>( Arrays.asList( new String[]{
      "tiny", "small", "medium", "large", "huge", "gargantuan" } ) );
   private final Set<String> keywords = new TreeSet<>();
   private final Set<String> sizes = new TreeSet<>();

   public MonsterConverter(Category category) {
      super(category);
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "Level", "CombatRole", "GroupRole", "Size", "CreatureType", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxType = Pattern.compile( "<span class=type>(.*?)</span>" ).matcher( "" );

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

         default:
            if ( find( "basic melee attack") ) {
               swap( "basic melee attack", "melee basic attack" );
               fix( "fix basic attack" );
            }
      }
      if ( find( regxType ) ) {
         do {
            String termText = regxType.group( 1 ).trim().toLowerCase().replaceAll( "[,()]+", "" );
            if ( ! termText.isEmpty() )
               keywords.addAll( Arrays.asList( termText.split( "\\s+" ) ) );
         } while ( regxType.find() );
         sizes.addAll( keywords );
         sizes.retainAll( AllSizes );
         keywords.removeAll( AllSizes );
         if ( sizes.isEmpty() ) {
            sizes.add( "medium" );
            fix( "missing content" );
         }
         meta( SIZE, String.join( ", ", sizes.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
         meta( TYPE, String.join( ", ", keywords.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
         sizes.clear();
         keywords.clear();
      } else
         warn( "Creature type not found" );
   }
}