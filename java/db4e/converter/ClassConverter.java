package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassConverter extends Converter {

   public ClassConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   private final Matcher regxClassFeatures = Pattern.compile( "<b>(?:Class Features?|Hybrid Talent Options?):? ?</b>:?([^<.]+)", Pattern.CASE_INSENSITIVE ).matcher( "" );
   static final Map<String, Set<String>> featureMap = new HashMap<>( 77, 1f );

   @Override protected void convertEntry( Entry entry ) {
      super.convertEntry( entry );
      entry.meta[2] = shortenAbility( entry.meta[2].toString() );

      regxClassFeatures.reset( entry.data );
      synchronized ( featureMap ) {
         Set<String> features = featureMap.get( entry.shortid );
         if ( features == null ) featureMap.put( entry.shortid, features = new HashSet<>() );
         while ( regxClassFeatures.find() ) {
            String[] names = regxClassFeatures.group( 1 ).trim().split( ",| or " );
            for ( String name : names ) {
               name = name.replaceFirst( "\\(.*\\)", "" ).trim();
               if ( name.endsWith( " Armor Proficiency" ) ) continue;
               features.add( name );
            }
         }
         if ( entry.name.toLowerCase().contains( "monk" ) )
            features.add( "Flurry of Blows" ); // Guess what? Flurry of Blows is not listed as a Monk feature, and there is no such power
         if ( features.isEmpty() )
            log.log( Level.WARNING, "Class features not found: {0} {1}", new Object[]{ entry.shortid, entry.name });
      }
   }

   @Override protected void correctEntry( Entry entry ) {
      switch ( entry.shortid ) {
         case "class811": // Assassin (Executioner)
         case "class891": // Hybrid Assassin (Executioner)
            entry.meta[1] = "Martial and Shadow";
            corrections.add( "meta" );
            break;
         case "class788": // Ranger (Hunter)
         case "class790": // Ranger (Scout)
         case "class906": // Barbarian (Berserker)
            entry.meta[1] = "Martial and Primal";
            corrections.add( "meta" );
            break;
         case "class907": // Bard (Skald)
            entry.meta[1] = "Arcane and Martial";
            corrections.add( "meta" );
            break;
         case "class893": // Hybrid Vampire
            entry.data = entry.data.replace( "per Day</b>: 2<", "per Day</b>: As a hybrid vampire, you gain two healing surges regardless of the class that you have combined with vampire to create your character.<" );
            corrections.add( "missing content" );
            // Fall-through
         case "class892": // Hybrid Blackguard
         case "class894": // Hybrid Sentinel
         case "class895": // Hybrid Cavalier
         case "class896": // Hybrid Binder
            entry.data = entry.data.replace( "Dragon Magazine 402", "Dragon Magazine 400" );
            corrections.add( "typo" );
            break;
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      String name = entry.name, altName = null;
      boolean isHybrid = name.startsWith( "Hybrid " );
      if ( isHybrid ) name = name.substring( 7 );
      if ( name.indexOf( '(' ) > 0 ) {
         altName = name.substring( name.indexOf( '(' ) + 1, name.length() - 1 );
         name = name.substring( 0, name.indexOf( '(' ) - 1 );
      }
      synchronized ( featureMap ) {
         Set<String> result = new HashSet<>( ClassConverter.featureMap.get( entry.shortid ) );
         result.add( name );
         if ( altName != null ) result.add( altName );
         if ( isHybrid ) result.add( "Hybrid " + name );
         if ( isHybrid && altName != null ) result.add( "Hybrid " + altName );
         return result.toArray( new String[ result.size() ] );
      }
   }
}