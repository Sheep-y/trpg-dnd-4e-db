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
   private final int KEYWORDS = 4;

   public PowerConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise () {
      category.meta = new String[]{ "ClassName", "Level", "Type", "Action", "Keywords", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxKeywords = Pattern.compile( "✦     (<b>[\\w ]+</b>(?:, <b>[\\w ]+</b>)*)" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "<span class=level>([^<]+) (Racial )?(Attack|Utility|Feature|Pact Boon|Cantrip)( \\d+)?" ).matcher( "" );

   @Override protected void convertEntry () {
      Object[] fields = entry.getFields();
      meta( fields[0], fields[1], "", fields[2], "", fields[3] );
      super.convertEntry();

      if ( ! find( regxLevel ) )
         warn( "Power without type" );

      // Add skill name to skill power type
      if ( meta( CLASS ).equals( "Skill Power" ) )
         entry.setField( CLASS, meta( CLASS ) + ", " + regxLevel.group( 1 ) );
      else if ( meta( CLASS ).equals( "Theme Power" ) ) {
         meta( CLASS, regxLevel.group( 1 ) );
         fix( "wrong meta" );
      }

      // Set frequency part of power type, a new column
      if ( entry.getContent().startsWith( "<h1 class=dailypower>" ) )
         meta( TYPE, "Daily" );
      else if ( entry.getContent().startsWith( "<h1 class=encounterpower>" ) )
         meta( TYPE, "Encounter" );
      else if ( entry.getContent().startsWith( "<h1 class=atwillpower>" ) )
         meta( TYPE, "At-Will" );
      else
         warn( "Power with unknown frequency" );

      // Set type part of power type column
      switch ( regxLevel.group( 3 ) ) {
         case "Attack":
            meta( TYPE, meta( TYPE ) + " Attack" );
            break;
         case "Cantrip":
         case "Utility":
            meta( TYPE, meta( TYPE ) +" Utility" );
            break;
         default:
            meta( TYPE, meta( TYPE ) +" Feature" );
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
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry () {
      if ( entry.getName().endsWith( " [Attack Technique]" ) ) {
         entry.setName( entry.getName().substring( 0, entry.getName().length() - 19 ) );
         fix( "wrong meta" );
      }

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
   }
}