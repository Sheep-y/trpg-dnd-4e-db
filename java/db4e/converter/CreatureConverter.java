package db4e.converter;

import db4e.data.Category;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class CreatureConverter extends LeveledConverter {

   protected int SIZE;
   protected int TYPE;

   private final Set<String> AllSizes = new HashSet<>( Arrays.asList( new String[]{
      "tiny", "small", "medium", "large", "huge", "gargantuan" } ) );
   protected final Set<String> KeywordBlackList = new HashSet<>();

   private final Set<String> keywords = new TreeSet<>();
   private final Set<String> sizes = new TreeSet<>( CreatureConverter::sizeSort );

   public CreatureConverter ( Category category ) {
      super( category );
   }

   private final Matcher regxType = Pattern.compile( "<span class=type>(.*?)(,\\s*+[^<()]++)?</span>" ).matcher( "" );

   protected boolean findSizeAndTypes () {
      if ( ! find( regxType ) ) return false;

      String termText = regxType.group( 1 ).trim().toLowerCase().replaceAll( "[/,()]+", " " );
      if ( ! termText.isEmpty() ) // type and keywords
         append( keywords, termText.split( "\\s+" ) );
      String race = regxType.group( 2 ); // race, e.g. drow, human,
      if ( race != null ) {
         race = race.substring( 1 ).trim();
         if ( ! race.trim().contains( " " ) ) // Exclude rare races like "red dragon", "fang titan drake", "dark one", "mind flayer" etc.
            keywords.add( race );
      }

      sizes.addAll( keywords );
      sizes.retainAll( AllSizes );
      keywords.removeAll( AllSizes );
      keywords.removeAll( KeywordBlackList );
      if ( sizes.isEmpty() ) {
         sizes.add( "medium" );
         swap( "<span class=type>", "<span class=type>Medium ", "missing keyword" );
      }
      meta( SIZE, String.join( ", ", sizes.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
      meta( TYPE, String.join( ", ", keywords.stream().map( Utils::ucfirst ).toArray( String[]::new ) ) );
      sizes.clear();
      keywords.clear();
      return true;
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

   private static int sizeSort ( String a, String b ) {
      return sizeIndex( a ) - sizeIndex( b );
   }
}
