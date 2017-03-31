package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassConverter extends Converter {

   private static final int POWER = 1;
   private static final int ABILITY = 2;

   public ClassConverter ( Category category ) {
      super( category );
   }

   private final Matcher regxClassFeatures = Pattern.compile( "<b>(?:Class Features?|Hybrid Talent Options?):? ?</b>:?([^<.]+)", Pattern.CASE_INSENSITIVE ).matcher( "" );
   static final Map<String, Set<String>> featureMap = new HashMap<>( 77, 1f );

   @Override protected void convertEntry () {
      super.convertEntry();
      meta( ABILITY, shortenAbility( meta( ABILITY ) ) );
   }

   @Override protected void indexEntry() {
      regxClassFeatures.reset( entry.getContent() );
      synchronized ( featureMap ) {
         Set<String> features = featureMap.get( entry.getId() );
         if ( features == null ) featureMap.put( entry.getId(), features = new HashSet<>() );
         while ( regxClassFeatures.find() ) {
            String[] names = regxClassFeatures.group( 1 ).trim().split( ",| or " );
            for ( String name : names ) {
               name = name.replaceFirst( "\\(.*\\)", "" ).trim();
               if ( name.endsWith( " Armor Proficiency" ) ) continue;
               features.add( name );
            }
         }
         if ( entry.getName().toLowerCase().contains( "monk" ) )
            features.add( "Flurry of Blows" ); // Guess what? Flurry of Blows is not listed as a Monk feature, and there is no such power
         if ( features.isEmpty() )
            warn( "Class features not found" );
      }
   }


   @Override protected void correctEntry() {
      switch ( entry.getId() ) {
         case "class811": // Assassin (Executioner)
         case "class891": // Hybrid Assassin (Executioner)
            meta( POWER, "Martial and Shadow" );
            fix( "wrong meta" );
            break;
         case "class788": // Ranger (Hunter)
         case "class790": // Ranger (Scout)
         case "class906": // Barbarian (Berserker)
            meta( POWER, "Martial and Primal" );
            fix( "wrong meta" );
            break;
         case "class907": // Bard (Skald)
            meta( POWER, "Arcane and Martial" );
            fix( "wrong meta" );
            break;
         case "class893": // Hybrid Vampire
            swap( "per Day</b>: 2<", "per Day</b>: As a hybrid vampire, you gain two healing surges regardless of the class that you have combined with vampire to create your character.<" );
            fix( "missing content" );
            // Fall-through
         case "class892": // Hybrid Blackguard
         case "class894": // Hybrid Sentinel
         case "class895": // Hybrid Cavalier
         case "class896": // Hybrid Binder
            swap( "Dragon Magazine 402", "Dragon Magazine 400" );
            fix( "typo" );
            break;
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      String name = entry.getName(), altName = null;
      boolean isHybrid = name.startsWith( "Hybrid " );
      if ( isHybrid ) name = name.substring( 7 );
      if ( name.indexOf( '(' ) > 0 ) {
         altName = name.substring( name.indexOf( '(' ) + 1, name.length() - 1 );
         name = name.substring( 0, name.indexOf( '(' ) - 1 );
      }
      synchronized ( featureMap ) {
         Set<String> result = new HashSet<>( featureMap.get( entry.getId() ) );
         result.add( name );
         if ( altName != null ) result.add( altName );
         if ( isHybrid ) result.add( "Hybrid " + name );
         if ( isHybrid && altName != null ) result.add( "Hybrid " + altName );
         return result.toArray( new String[ result.size() ] );
      }
   }

   @Override protected int sortEntity(Entry a, Entry b) {
      boolean HybridA = a.getName().startsWith( "Hybrid" );
      boolean HybridB = b.getName().startsWith( "Hybrid" );
      if ( HybridA == HybridB ) return a.getName().compareTo( b.getName() );
      return HybridA ? 1 : -1;
   }
}