package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
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
      category.meta = new String[]{ "Type", "Campaign", "Benefit", "SourceBook" };
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

      if ( meta( 1 ).equals( "Scales of War Adventure Path" ) )
         meta( CAMPAIGN, "Scales of War" );
      else if ( meta( 1 ).equals( "Forgotten Realms" ) )
         meta( CAMPAIGN, "Faerûn" );
   }

   @Override protected void correctEntry () {
      if ( meta( TYPE ).isEmpty() && entry.display_name.contains( " - " ) ) {
         meta( TYPE, entry.display_name.split( " - " )[0] );
         fix( "missing meta" );
      }

      if ( find( regxAnyLang ) ) {
         entry.data = regxAnyLang.replaceFirst( "Any language except " );
         fix( "consistency" );
      }

      if ( find( regxAssociate ) ) { // Skill or language
         String associate = regxAssociate.group( 1 );
         if ( regxAssociate.find() ) // Language
            associate += ", " + regxAssociate.group( 1 );
         //if ( ! assoc.equals( meta( BENEFIT ) ) ) fix( "wrong meta" ); // The skill column always miss language, but can't blame them
         meta( BENEFIT, "Associated: " + associate );
      } else if ( ! meta( BENEFIT ).isEmpty() )
         meta( BENEFIT, "Associated: " + meta( BENEFIT ).replace( "you can", "" ) );

      switch ( entry.shortid ) {
         case "background34": // Warsmith
            meta( BENEFIT, "Can construct weapons and armor. Can cast Creation rituals as if you have Ritual Caster." );
            break;
         case "background77": // Imbuer
            meta( BENEFIT, "Can construct implements and wondrous items. Can cast Creation rituals as if you have Ritual Caster." );
            break;
         case "background276": // Windrise Ports
            meta( BENEFIT, "Add one skill to your class skill list, and gain one additional language." );
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
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      return new String[]{ entry.name };
   }

}