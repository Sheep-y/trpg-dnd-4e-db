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
   private final Set<String> KeywordBlackList = new HashSet<>( Arrays.asList( new String[]{
      "female", "or" } ) ); // Female: monster115831 (x3), Or: monster1466 (x1)

   private final Set<String> keywords = new TreeSet<>();
   private final Set<String> sizes = new TreeSet<>( MonsterConverter::sizeSort );

   public MonsterConverter(Category category) {
      super(category);
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "Level", "CombatRole", "GroupRole", "Size", "CreatureType", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxType = Pattern.compile( "<span class=type>(.*?)(,\\s*[^<()]+)?</span>" ).matcher( "" );

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
            break;

         default:
            if ( find( "basic melee attack") ) {
               swap( "basic melee attack", "melee basic attack" );
               fix( "fix basic attack" );
            }
      }
      if ( find( regxType ) ) {
         do {
            String termText = regxType.group( 1 ).trim().toLowerCase().replaceAll( "[/,()]+", " " );
            if ( ! termText.isEmpty() ) // type and keywords
               keywords.addAll( Arrays.asList( termText.split( "\\s+" ) ) );
            String race = regxType.group( 2 ); // race, e.g. drow, human,
            if ( race != null ) {
               race = race.substring( 1 ).trim();
               if ( ! race.trim().contains( " " ) ) // Exclude rare races like "red dragon", "fang titan drake", "dark one", "mind flayer" etc.
                  keywords.add( race );
            }
         } while ( regxType.find() );
         sizes.addAll( keywords );
         sizes.retainAll( AllSizes );
         keywords.removeAll( AllSizes );
         keywords.removeAll( KeywordBlackList );
         if ( sizes.isEmpty() ) {
            sizes.add( "medium" );
            swap( "<span class=type>", "<span class=type>Medium " );
            fix( "missing content" );
         }
         meta( SIZE, String.join( ", ", sizes.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
         meta( TYPE, String.join( ", ", keywords.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
         sizes.clear();
         keywords.clear();
      } else
         warn( "Creature type not found" );
   }

   private static final int sizeIndex ( String size ) {
      switch ( size.toLowerCase() ) {
         case "tiny":
            return 1;
         case "small":
            return 2;
         case "medium":
            return 3;
         case "large":
            return 4;
         case "huge":
            return 5;
         case "gargantuan":
            return 6;
         default:
            return 0;
      }
   }

   private static final int sizeSort ( String a, String b ) {
      return sizeIndex( a ) - sizeIndex( b );
   }
}