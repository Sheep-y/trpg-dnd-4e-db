package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.HashSet;
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

   private Set<String> findFeatures () {
      regxClassFeatures.reset( entry.getContent() );
      Set<String> features = new HashSet<>( 16, 1.0f );
      while ( regxClassFeatures.find() ) {
         String[] names = regxClassFeatures.group( 1 ).trim().split( ",| or " );
         for ( String name : names ) {
            name = name.replaceFirst( "\\(.*\\)", "" ).trim();
            if ( name.endsWith( " Armor Proficiency" ) || name.equals( "Ritual Casting" ) ) continue;
            features.add( name );
            test( LOOKUP, name );
         }
      }
      if ( entry.getName().toLowerCase().contains( "monk" ) ) {
         features.add( "Flurry of Blows" ); // Guess what? Flurry of Blows is not listed as a Monk feature, and there is no such power
         test( LOOKUP, "Flurry of Blows" );
      }
      if ( features.isEmpty() )
         warn( "Class features not found" );
      return features;
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
      switch ( entry.getId() ) {
         case "class148": // Barbarian
         case "class906": // Barbarian (Berserker)
         case "class440": // Hybrid Barbarian
         case "class713": // Fighter (Knight)
         case "class716": // Fighter (Slayer)
         case "class3": // Fighter (Weaponmaster)
         case "class353": // Hybrid Fighter
         case "class813": // Paladin (Blackguard)
         case "class784": // Paladin (Cavalier)
         case "class602": // Runepriest
         case "class611": // Hybrid Runepriest
         case "class134": // Warden
         case "class446": // Hybrid Warden
         case "class8": // Warlord (Marshal)
         case "class359": // Hybrid Warlord
            meta( ABILITY, "Str" );
            break;
         case "class124": // Battlemind
         case "class590": // Hybrid Battlemind
            meta( ABILITY, "Con" );
            break;
         case "class466": // Assassian
         case "class811": // Assassian (Executioner)
         case "class641": // Hybrid Assassian
         case "class891": // Hybrid Assassian (Executioner)
         case "class362": // Monk
         case "class609": // Hybrid Monk
         case "class790": // Ranger (Scout)
         case "class6": // Rogue (Scoundrel)
         case "class356": // Hybrid Rogue
         case "class817": // Vampire
         case "class893": // Hybrid Vampire
            meta( ABILITY, "Dex" );
            break;
         case "class125": // Artificer
         case "class536": // Hybrid Artificer
         case "class437": // Psion
         case "class610": // Hybrid Psion
         case "class53": // Swordmage
         case "class357": // Hybrid Swordmage
         case "class9": // Wizard (Arcanist)
         case "class883": // Wizard (Bladesinger)
         case "class722": // Wizard (Mage)
         case "class958": // Wizard (Sha'ir)
         case "class908": // Wizard (Witch)
         case "class360": // Hybrid Wizard
            meta( ABILITY, "Int" );
            break;
         case "class129": // Avenger
         case "class439": // Hybrid Avenger
         case "class705": // Cleric (Warpriest)
         case "class126": // Druid
         case "class909": // Druid (Protector)
         case "class779": // Druid (Sentinel)
         case "class442": // Hybrid Druid
         case "class894": // Hybrid Druid (Sentinel)
         case "class127": // Invoker
         case "class443": // Hybrid Invoker
         case "class472": // Seeker
         case "class612": // Hybrid Seeker
         case "class147": // Shaman
         case "class444": // Hybrid Shaman
            meta( ABILITY, "Wis" );
            break;
         case "class529": // Ardent
         case "class588": // Hybrid Ardent
         case "class104": // Bard
         case "class907": // Bard (Skald)
         case "class441": // Hybrid Bard
         case "class128": // Sorcerer
         case "class956": // Sorcerer (Elementalist)
         case "class445": // Hybrid Sorcerer
         case "class821": // Warlock (Binder)
         case "class793": // Warlock (Hexblade)
            meta( ABILITY, "Cha" );
            break;
         case "class5": // Ranger
         case "class355": // Hybrid Ranger
            meta( ABILITY, "Str, Dex" );
            break;
         case "class2": // Cleric (Templar)
         case "class352": // Hybrid Cleric
            meta( ABILITY, "Str, Wis" );
            break;
         case "class4": // Paladin
         case "class354": // Hybrid Paladin
         case "class892": // Hybrid Paladin (Blackguard)
         case "class895": // Hybrid Paladin (Cavalier)
            meta( ABILITY, "Str, Cha" );
            break;
         case "class788": // Ranger (Hunter)
         case "class719": // Rogue (Thief)
            meta( ABILITY, "Dex or Str" );
            break;
         case "class7": // Warlock
         case "class358": // Hybrid Warlock
         case "class896": // Hybrid Warlock (Binder)
            meta( ABILITY, "Con, Cha" );
            break;
         default:
            warn( "Unknown class, cannot provide primary abilities" );
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
      Set<String> result = findFeatures();
      result.add( name );
      test( LOOKUP, name );
      if ( altName != null ) result.add( altName );
      if ( isHybrid ) result.add( "Hybrid " + name );
      if ( isHybrid && altName != null ) result.add( "Hybrid " + altName );
      return result.toArray( new String[ result.size() ] );
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      boolean HybridA = a.getName().startsWith( "Hybrid" );
      boolean HybridB = b.getName().startsWith( "Hybrid" );
      if ( HybridA == HybridB ) return a.getName().compareTo( b.getName() );
      return HybridA ? 1 : -1;
   }
}