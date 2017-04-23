package db4e.converter;

import db4e.Main;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class BackgroundConverter extends Converter {

   private static final int TYPE = 0;
   private static final int CAMPAIGN = 1;
   private static final int BENEFIT = 2;

   public BackgroundConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise() {
      category.fields = new String[]{ "Type", "Campaign", "Benefit", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxAssociate  = Pattern.compile( "<i>Associated (?:Skills?|Languages?): </i>([^<(.]+)" ).matcher( "" );
   private final Matcher regxBenefit  = Pattern.compile( "<i>Benefit: </i>([^<(.]+)" ).matcher( "" );
   private final Matcher regxAnyLang  = Pattern.compile( "Any (one|language) (other than|except) " ).matcher( "" );
   private final String[] trims = { "list of", "list", "checks?", "bonus", "additional",
      "if you[^,]+, ", " rather than your own", "of your choice", ", allowing[^:]+:",
      "you gain a", "you are", "(?<!kill )your?",
      ", but you must keep the second result(, even if it is worse)?" };
   private final Matcher regxTrim  = Pattern.compile( "(?:\\s|\\b)(?:" + String.join( "|", trims ) + ")\\b", Pattern.CASE_INSENSITIVE ).matcher( "" );

   @Override protected void convertEntry () {
      super.convertEntry();
      if ( meta( BENEFIT ).isEmpty() && find( regxBenefit ) ) {
         String associate = regxTrim.reset( regxBenefit.group( 1 ) ).replaceAll( "" ).trim();
         if ( ! associate.endsWith( "." ) ) associate += '.';
         associate = associate.replace( "saving throws", "saves" );
         meta( BENEFIT, Utils.ucfirst( associate ) );
      }

      if ( meta( CAMPAIGN ).equals( "Scales of War Adventure Path" ) )
         meta( CAMPAIGN, "Scales of War" );
      else if ( meta( CAMPAIGN ).equals( "Forgotten Realms" ) )
         meta( CAMPAIGN, "Faerûn" );
   }

   @Override protected void correctEntry () {
      if ( meta( TYPE ).isEmpty() && entry.getName().contains( " - " ) ) {
         meta( TYPE, entry.getName().split( " - " )[0] );
         fix( "missing meta" );
      }

      if ( find( regxAnyLang ) ) {
         entry.setContent( regxAnyLang.replaceFirst( "Any language except " ) );
         fix( "consistency" );
      }

      if ( find( regxAssociate ) ) { // Skill or language
         String associate = regxAssociate.group( 1 );
         if ( regxAssociate.find() ) // Language
            associate += ", " + regxAssociate.group( 1 );
         if ( ! entry.getId().equals( "background232" ) )
            meta( BENEFIT, "Associated: " + associate );
      }

      switch ( entry.getId() ) {
         case "background30": // Scorned Noble
            meta( BENEFIT, "Gain +2 to saves when no allies are within 5 squares." );
            break;
         case "background34": // Warsmith
            meta( BENEFIT, "Can construct weapons and armor. Can cast Creation rituals as if you have Ritual Caster." );
            break;
         case "background77": // Imbuer
            meta( BENEFIT, "Can construct implements and wondrous items. Can cast Creation rituals as if you have Ritual Caster." );
            break;
         case "background163": // Accursed Lineage
            metaAdd( BENEFIT, ". +2 Diplomacy and Intimidate against undead." );
            break;
         case "background164": // Adventurer's Scion
            metaAdd( BENEFIT, ". Can reroll any monster knowledge checks." );
            break;
         case "background165": // Curious Archeologist
            metaAdd( BENEFIT, ". first successful Thievery check to disable trap in a skill challenge grants an extra success." );
            break;
         case "background166": // Lost Kin
            metaAdd( BENEFIT, ". +3 bonus to Natural checks made to recall geography." );
            break;
         case "background167": // Necromancer's Chattel
            metaAdd( BENEFIT, ". +2 to save against fear effects." );
            break;
         case "background168": // Restless Dead
            metaAdd( BENEFIT, ". +1 to damage rolls against undead." );
            break;
         case "background169": // Touched by Darkness
            metaAdd( BENEFIT, ". +1 to saves against necrotic effects." );
            break;
         case "background232": // Child of Two Worlds
            meta( BENEFIT, "Associated: choose from either region of your origin.");
            break;
         case "background276": // Windrise Ports
            meta( BENEFIT, "Add one skill to your class skill list, and gain one additional language." );
            break;
         case "background570": // Urban Shaman
            metaAdd( BENEFIT, ". Substitude Streetwise for Nature on rituals when in urban." );
            break;

         case "background747": // Baldur's Gate (Shifter)
            meta( BENEFIT, "Add History to class skill. +2 bonus to History. Add Elven to known languages. Pass as Elf when not shifting." );
            break;
         case "background748": // The Dalelands (Shifter)
            meta( BENEFIT, "Add Stealth and Nature to class skill. +2 bonus to Stealth. While shifting, +2 bonus to Intimidate." );
            break;
         case "background750": // Durpar (Shifter)
            meta( BENEFIT, "Reroll any Dungeoneering. Add Goblin to known languages." );
            break;
         case "background751": // The Great Dale (Shifter)
            meta( BENEFIT, "Reroll any Nature. Add Elven to known languages." );
            break;
         case "background753": // Luskan (Shifter)
            meta( BENEFIT, "Reroll any Stealth. Add Shou to known languages." );
            break;
         case "background754": // Moonshae Isles (Shifter)
            meta( BENEFIT, "Add Athletics to class skill. +3 bonus to Athletics checks made to swim, piloting water vessels, or move on boat. Add Chondathan to known languages." );
            break;
         case "background756": // Sembia (Shifter)
            meta( BENEFIT, "Add Bluff and Diplomacy to class skill. Add Netherese to known languages. Pass as Human when not shifting." );
            break;
         case "background757": // Tethyr (Shifter)
            meta( BENEFIT, "Add Stealth to class skill. +2 bonus to Stealth. Add Chondathan or Draconic to known languages." );
            break;

         default:
            if ( Main.debug.get() && meta( BENEFIT ).startsWith( "Associated:" ) && find( regxBenefit ) )
               log.log( Level.WARNING, "Benefits in additoin to associated skills: {0}", entry );
      }
      super.correctEntry();
   }

   @Override protected Set<String> getLookupName ( Entry entry, Set<String> list ) {
      return appendList( list, entry.getName().contains( " - " )
         ? entry.getName().split( " - " )[1]
         : entry.getName() );
   }
}