package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

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

   private final Matcher regxLevel     = Pattern.compile( "<span class=level>([^<]+?) (Racial )?+(Attack|Utility|Feature|Pact Boon|Cantrip){1}+( \\d++)?" ).matcher( "" );
   private final Matcher regxKeywords  = Pattern.compile( "✦     <b>(.*?)<br>" ).matcher( "" );
   private final Matcher regxKeywordGain  = Pattern.compile( "(?<=<b>Keyword</b>: )Th(?:is|e) power gains the (\\w++) keyword\\." ).matcher( "" );
   private final Matcher regxKeywordSection  = Pattern.compile( "<b>Keyword</b>: ([^<]++)" ).matcher( "" );
   private final Matcher regxAction    = Pattern.compile( "\\b(Action|Interrupt){1}+</b>\\s*" ).matcher( "" );
   private final Matcher regxRangeType = Pattern.compile( "<b>(Melee(?:(?: touch)?+ or Ranged)?+|Ranged|Close|Area|Personal|Special){1}+</b>(?!:)([^<]*+)" ).matcher( "" );
   private final Set<String> RangeSubtype = new HashSet<>( Arrays.asList( "Ranged", "blast", "burst", "close", "sight", "wall" ) );

   private final Set<String> keywords = new ConcurrentSkipListSet<>();

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( fields[0], fields[1], "", fields[2], "", "" );
      super.convertEntry();

      // Add skill name to skill power type
      locate( regxLevel );
      if ( meta( CLASS ).equals( "Skill Power" ) )
         metaAdd( CLASS, ", " + regxLevel.group( 1 ) );
      else if ( meta( CLASS ).equals( "Theme Power" ) ) {
         meta( CLASS, regxLevel.group( 1 ) );
         fix( "wrong class" );
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

      // New column: keywords
      if ( find( "✦" ) ) {
         if ( find( regxKeywords ) ) {
            do {
               append( keywords, regxHtmlTag.reset( regxKeywords.group( 1 ) ).replaceAll( "" ).split( "\\s*+(?:[;,]|or)\\s++" ) );
            } while ( regxKeywords.find() );
         } else {
            // Deathly Glare, Hamadryad Aspects, and Flock Tactics have bullet star in body but not keywords.
            if ( ! entry.getId().equals( "power12521" ) && ! entry.getId().equals( "power15829" ) && ! entry.getId().equals( "power16541" ) )
               warn( "Power without keywords" );
         }
      }
      if ( find( "<b>Keyword</b>:" ) ) {
         locate( regxKeywordSection );
         append( keywords, regxKeywordSection.group( 1 ) ); // All power with multiple keyword sections has same keyword for both sections
      }
      if ( find( regxRangeType ) ) {
         do { // Some power have multiple keyword lines.
            String range = regxRangeType.group( 1 );
            String area = regxRangeType.group( 2 );
            switch ( range ) {
               case "Melee touch or Ranged":
                  swap( "Melee touch or Ranged", "Melee</b> touch<b> or Ranged", "styling" );
                  // fallthrough
               case "Melee or Ranged":
                  append( keywords, "Melee", "Ranged" );
                  break;
               case "Close":
               case "Area":
                  if ( area == null )
                     warn( range + " without area type" );
               default:
                  keywords.add( range );
            }
            if ( area != null )
               Arrays.stream( area.trim().split( "\\s+" ) ).filter( RangeSubtype::contains ).map( Utils::ucfirst ).forEach( keywords::add );
         } while( regxRangeType.find() );
      } else {
         if ( entry.getId().equals( "power16338" ) ) { // Elemental Cascade
            swap( "Melee 1 or Ranged 10", "Melee</b> 1<b> or Ranged</b> 10", "styling" );
            append( keywords, "Melee", "Ranged" );
         } else
            warn( "Rangeless power" );
      }
      if ( ! keywords.isEmpty() ) {
         meta( KEYWORDS, String.join( ", ", keywords.toArray( new String[ keywords.size() ] ) ) );
         keywords.clear();
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

   private final Matcher regxRangeFix = Pattern.compile( "<b>(Close|Area){1}+\\s*+(burst|blast){1}+</b>(?!:)" ).matcher( "" );

   @Override protected void correctEntry () {
      if ( entry.getName().endsWith( " [Attack Technique]" ) ) {
         entry.setName( entry.getName().substring( 0, entry.getName().length() - 19 ) );
         fix( "wrong meta" );
      }

      if ( meta( ACTION ).startsWith( "Immediate " ) )
         meta( ACTION, meta( ACTION ).replace( "Immediate ", "Imm. " ) );

      if ( find( regxRangeFix ) ) {
         swapAll( regxRangeFix.group(), "<b>" + regxRangeFix.group( 1 ) + "</b> " + regxRangeFix.group( 2 ) );
         fix( "styling" );
      }

      switch ( entry.getId() ) {
         case "power3660": // Indomitable Resolve
            swapFirst( "<br>", "" ); // <br> in flavor text
            fix( "formatting" );
            break;

         case "power4155": // Iron-Hide Infusion
            swap( "Burst 5", "burst 5", "typo" );
            break;

         case "power4699": // Elemental Chaos Smite
            swap( "weaopn", "weapon", "typo" );
            break;

         case "power4713": // Lurk Unseen
            swap( ">Wildcat Stalker 12<", ">Wildcat Stalker Utility 12<", "missing keyword" );
            break;

         case "power5767": // Marksman's Vision
            swap( "weapoon", "weapon", "typo" );
            break;

         case "power5826": // Summon Bonded Protector
            swap( ", <b>Teleportation</b>", "", "missing keyword" ); // Actually *removing* keyword
            break;

         case "power6595": // Bane's Tactics
            swap( "basic melee attack", "melee basic attack", "fix basic attack" );
            break;

         case "power7358": // Death's Messenger
         case "power7361": // Sudden Retaliation
         case "power7363": // Quick Kill
         case "power7367": // Improvised Poison
         case "power7369": // Progressive Toxin
            addRange( "<b>Melee or Ranged</b>" );
            break;

         case "power8278": // Dark Reaping
         case "power10239": // Punitive Radiance
         case "power10418": // Familiar Harrier
         case "power12471": // Protective Familiar
            addRange( "<b>Special</b>" );
            break;

         case "power9331": // Spot the Path
            swap( ": :", ":", "typo" );
            break;

         case "power9348": // Bending Branch
            swap( "<p class=powerstat>   ✦", "<p class=powerstat><b>Encounter</b>   ✦", "missing power frequency" );
            break;

         case "power11757": // Unwavering Vigilance"
            addRange( "<b>Ranged</b> sight" );
            swap( "sight  </p>", "sight</p>" ); // Trim additional spaces
            break;

         case "power11853": // Summon Djinn Stormcaller
            swap( ", gain", ", you gain", "typo" );
            break;

         case "power12184": // Animus Strike
         case "power12880": // Cloudburst
            swap( "Primal</b>", "Primal</b>, <b>Spirit</b>", "missing keyword" );
            break;

         case "power12455": // Vaporous Step
            addRange( "<b>Close</b> burst 5" );
            swap( "each ally within 5 squares of you", "each ally in the burst", "consistency" );
            break;

         case "power13769": // Command Undead
            swap( "Close</b> 5", "Close</b> burst 5", "missing keyword" );
            break;

         case "power15829": // Hamadryad Aspects
            swap( "</p><p class=powerstat>  <b>✦", "<br>  <b>✦" );
            swap( "</p><p class=flavor>  <b>✦", "<br>  <b>✦", "formatting" );
            break;

         case "power16604": // Spectral Forest
         case "power14519": // Verdant Retaliation
         case "power15868": // Terror of the Dark Moon
            swap( "<b>Aura</b> burst", "<b>Area</b> burst", "typo" );
            break;

         case "power4311" : // Darkspiral Aura
         case "power6017" : // Lawbreaker's Doom
         case "power6969" : // Quickened Coercion
         case "power7360" : // Bravo's Finish
         case "power7429" : // Wizard's Fury
         case "power7430" : // Learned Boost
         case "power7432" : // Master's Surge
         case "power9295" : // Agile Recovery
         case "power12286": // Deadly Visions
         case "power12451": // Undeniable Tenacity
         case "power12452": // Contrivance of Speed
         case "power12454": // Shadow Adept [teleport]
         case "power12460": // Precision Gait
         case "power13431": // Hidden Strike
         case "power16695": // River Rat's Gambit
            addRange( "<b>Personal</b>" );
            break;

         case "power11331": // Crystalline Bonds
            locate( regxKeywordGain );
            swap( regxKeywordGain.group(), Utils.ucfirst( regxKeywordGain.group( 1 ) ) );
            // Fallthrough
         case "power10287": // Courageous Strike
         case "power10294": // Revelatory Strike
         case "power10299": // Diamond Defense Assault
         case "power10305": // Perception Shift
         case "power10311": // Dismissive Strike
         case "power10445": // Step of the Pursuer
         case "power10446": // Battle Vortex
         case "power11057": // Irruption of the Living Gate
         case "power11123": // Confusing Strike
         case "power11146": // Unnerving Disruption
         case "power11326": // Thunder Tether
            locate( regxKeywordGain );
            String keyword = Utils.ucfirst( regxKeywordGain.group( 1 ) );
            swap( regxKeywordGain.group(), keyword, "consistency" );
            test( TEXT, "<b>Keyword</b>: " + keyword );
            test( KEYWORDS, keyword );
            break;

         default:
            if ( find( "Racial Power" ) ) {
               if ( find( "<p class=powerstat><b>Attack</b>" ) )
                  swap( "Racial Power", "Racial Attack", "consistency" );
               else
                  swap( "Racial Power", "Racial Utility", "consistency" );
            }

            if ( meta( CLASS ).equals( "Multiclass" ) ) {
               meta( CLASS, "Spellscarred" );
               fix( "wrong class" );
            }
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

   private void addRange ( String range ) {
      locate( regxAction );
      swap( regxAction.group(), regxAction.group( 1 ) + "</b>      " + range, "missing keyword" );
   }
}