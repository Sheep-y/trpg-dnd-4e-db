package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerConverter extends LeveledConverter {

   private final int CLASS = 0;
   private final int TYPE = 2;
   private final int KEYWORDS = 4;

   public PowerConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise () {
      category.meta = new String[]{ "ClassName", "Level", "Type", "Action", "Keywords", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxKeywords = Pattern.compile( "✦     (<b>[\\w ]+</b>(?:, <b>[\\w ]+</b>)*)" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "<span class=level>([^<]+) (Attack|Utility|Feature|Racial Power|Pact Boon|Cantrip)( \\d+)?" ).matcher( "" );

   @Override protected void convertEntry ( Entry entry ) {
      entry.meta = new Object[]{ entry.fields[0], entry.fields[1], "", entry.fields[2], "", entry.fields[3] };
      super.convertEntry( entry );
      final String data = entry.data;

      if ( ! regxLevel.reset( data ).find() )
         log.log( Level.WARNING, "Power without type: {0} {1}", new Object[]{ entry.shortid, entry.name } );

      // Add skill name to skill power type
      if ( entry.meta[ CLASS ].equals( "Skill Power" ) )
         entry.meta[ CLASS ] += ", " + regxLevel.group( 1 );

      // Set power frequency, a new column
      if ( data.startsWith( "<h1 class=dailypower>" ) )
         entry.meta[ TYPE ] = "Daily";
      else if ( data.startsWith( "<h1 class=encounterpower>" ) )
         entry.meta[ TYPE ] = "Encounter";
      else if ( data.startsWith( "<h1 class=atwillpower>" ) )
         entry.meta[ TYPE ] = "At-Will";
      else
         log.log( Level.WARNING, "Power with unknown frequency: {0} {1}", new Object[]{ entry.shortid, entry.name } );

      // Set keyword, a new column
      if ( data.indexOf( '✦' ) >= 0 ) {
         if ( regxKeywords.reset( data ).find() ) {
            Set<String> keywords = new HashSet<>(8); // Some power have multiple keyword lines.
            do {
               keywords.addAll( Arrays.asList( regxKeywords.group( 1 ).replaceAll( "</?b>", "" ).split( ", " ) ) );
            } while ( regxKeywords.find() );
            entry.meta[ KEYWORDS ] = String.join( ", ", keywords.toArray( new String[ keywords.size() ] ) );
         } else {
            // Deathly Glare, Hamadryad Aspects, and Flock Tactics have bullet star in body but not keywords.
            if ( ! entry.shortid.equals( "power12521" ) && ! entry.shortid.equals( "power15829" ) && ! entry.shortid.equals( "power16541" ) )
               log.log( Level.WARNING, "Power with no keywords: {0} {1}", new Object[]{ entry.shortid, entry.name } );
         }
      }
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.meta[ CLASS ].toString().compareTo( b.meta[ CLASS ].toString() );
      if ( diff != 0 ) return diff;
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry( Entry entry ) {
      if ( entry.data.contains( " [Attack Technique]" ) )
         corrections.add( "meta" ); // Fixed in Convert.beforeConvert

      switch ( entry.shortid ) {
         case "power4713": // Lurk Unseen
            entry.data = entry.data.replace( ">Wildcat Stalker 12<", ">Wildcat Stalker Utility 12<" );
            corrections.add( "missing content" );
            break;

         case "power6595": // Bane's Tactics
            entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "power9331": // Spot the Path
            entry.data = entry.data.replace( ": :", ":" );
            corrections.add( "typo" );
            break;

         case "power15829": // Hamadryad Aspects
            entry.data = entry.data.replace( "</p><p class=powerstat>  <b>✦", "<br>  ✦" );
            entry.data = entry.data.replace( "</p><p class=flavor>  <b>✦", "<br>  ✦" );
            corrections.add( "formatting" );
            break;
      }
   }
}