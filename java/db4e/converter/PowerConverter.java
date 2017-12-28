package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerConverter extends LeveledConverter {

   private final int CLASS = 0;
   private final int TYPE = 2;
   private final int ACTION = 3;
   private final int KEYWORDS = 4;

   public PowerConverter ( Category category ) {
      super( category );
      compileFlavorBr();
   }

   @Override protected void initialise () {
      category.fields = new String[]{ "ClassName", "Level", "Type", "Action", "Keywords", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxKeywords = Pattern.compile( "✦     (<b>[\\w ]++</b>(?:, <b>[\\w ]++</b>)*+)" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "<span class=level>([^<]+?) (Racial )?+(Attack|Utility|Feature|Pact Boon|Cantrip){1}+( \\d++)?" ).matcher( "" );

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( fields[0], fields[1], "", fields[2], "", fields[3] );
      super.convertEntry();

      if ( ! find( regxLevel ) )
         warn( "Power without type" );

      // Add skill name to skill power type
      if ( meta( CLASS ).equals( "Skill Power" ) )
         metaAdd( CLASS, ", " + regxLevel.group( 1 ) );
      else if ( meta( CLASS ).equals( "Theme Power" ) ) {
         meta( CLASS, regxLevel.group( 1 ) );
         fix( "wrong meta" );
      }

      // Set frequency part of power type, a new column
      if ( data().startsWith( "<h1 class=dailypower>" ) ) {
         meta( TYPE, "Daily" );
      } else if ( data().startsWith( "<h1 class=encounterpower>" ) ) {
         meta( TYPE, "Enc." );
      } else if ( data().startsWith( "<h1 class=atwillpower>" ) ) {
         meta( TYPE, "At-Will" );
      } else
         warn( "Power with unknown frequency" );

      // Set type part of power type column
      switch ( regxLevel.group( 3 ) ) {
         case "Attack":
            metaAdd( TYPE, " Attack" );
            break;
         case "Cantrip":
         case "Utility":
            metaAdd( TYPE, " Utility" );
            break;
         default:
            metaAdd( TYPE, " Feature" );
      }

      // Set keyword, a new column
      if ( find( "✦" ) ) {
         if ( find( regxKeywords ) ) {
            Set<String> keywords = new HashSet<>(8); // Some power have multiple keyword lines.
            do {
               keywords.addAll( Arrays.asList( regxKeywords.group( 1 ).replaceAll( "</?b>", "" ).split( ", " ) ) );
            } while ( regxKeywords.find() );
            meta( KEYWORDS, String.join( ", ", keywords.toArray( new String[ keywords.size() ] ) ) );
         } else {
            // Deathly Glare, Hamadryad Aspects, and Flock Tactics have bullet star in body but not keywords.
            if ( ! entry.getId().equals( "power12521" ) && ! entry.getId().equals( "power15829" ) && ! entry.getId().equals( "power16541" ) )
               warn( "Power without keywords" );
         }
      }
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( CLASS ).compareTo( b.getSimpleField( CLASS ) );
      if ( diff != 0 ) return diff;
      diff = sortLevel( a, b );
      if ( diff != 0 ) return diff;
      if ( ! a.getSimpleField( CLASS ).contains( "Power" ) ) { // Feat Power. Skill Power, Wild Talent Power
         int aFreq = sortType( a.getSimpleField( TYPE ) ), bFreq = sortType( b.getSimpleField( TYPE ) );
         if ( aFreq != bFreq ) return aFreq - bFreq;
      }
      return a.getName().compareTo( b.getName() );
   }

   private int sortType ( String type ) {
      int result;
      if ( type.startsWith( "Daily" ) )
         result = 16;
      else if ( type.startsWith( "Encounter" ) )
         result = 8;
      else
         result = 0;
      if ( type.endsWith( "Attack" ) )
         result += 2;
      else if ( type.endsWith( "Utility" ) )
         result += 4;
      return result;
   }

   @Override protected void correctEntry () {
      if ( entry.getName().endsWith( " [Attack Technique]" ) ) {
         entry.setName( entry.getName().substring( 0, entry.getName().length() - 19 ) );
         fix( "wrong meta" );
      }

      if ( meta( ACTION ).startsWith( "Immediate " ) )
         meta( ACTION, meta( ACTION ).replace( "Immediate ", "Imm. " ) );

      switch ( entry.getId() ) {
         case "power3660": // Indomitable Resolve
            swapFirst( "<br>", "" ); // <br> in flavor text
            fix( "formatting" );
            break;

         case "power4713": // Lurk Unseen
            swap( ">Wildcat Stalker 12<", ">Wildcat Stalker Utility 12<" );
            fix( "missing content" );
            break;

         case "power6595": // Bane's Tactics
            swap( "basic melee attack", "melee basic attack" );
            fix( "fix basic attack" );
            break;

         case "power9331": // Spot the Path
            swap( ": :", ":" );
            fix( "typo" );
            break;

         case "power9348": // Bending Branch
            swap( "<p class=powerstat>   ✦", "<p class=powerstat><b>Encounter</b>   ✦" );
            fix( "missing power frequency" );
            break;

         case "power15829": // Hamadryad Aspects
            swap( "</p><p class=powerstat>  <b>✦", "<br>  <b>✦" );
            swap( "</p><p class=flavor>  <b>✦", "<br>  <b>✦" );
            fix( "formatting" );
            break;
      }

      if ( find( "Racial Power" ) ) {
         if ( find( "<p class=powerstat><b>Attack</b>" ) )
            swap( "Racial Power", "Racial Attack" );
         else
            swap( "Racial Power", "Racial Utility" );
         fix( "consistency" );
      }

      if ( meta( CLASS ).equals( "Multiclass" ) ) {
         meta( CLASS, "Spellscarred" );
         fix( "wrong meta" );
      }

      stripFlavorBr();
      super.correctEntry();

      switch ( entry.getId() ) {
         case "power2839" : // Globe of Invulnerability
         case "power4915" : // Feral Rejuvenation
         case "power5182" : // Awaken the Forest
         case "power7421" : // Transpose Familiar
         case "power7439" : // Winter's Blood
         case "power7493" : // Soul Dance
         case "power7571" : // Path of Light
         case "power7611" : // Shadowstep
         case "power9285" : // Just Punishment
         case "power9710" : // Fazing Fangs
         case "power9964" : // Boughs of the World Tree
         case "power10254": // Deathguide's Stance
         case "power10317": // Combined Effort
         case "power11311": // Vestige of Kulnoghrim
         case "power11328": // Imprison
         case "power11516": // Cloud of Doom
         case "power12191": // Shove and Slap
            fixPowerFrequency();
            break;
      }
   }
}