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
   private final int FREQUENCY = 2;
   private final int KEYWORDS = 4;

   public PowerConverter ( Category category, boolean debug ) {
      super( category, debug );
      category.meta = new String[]{ "ClassName", "Level", "Frequency", "Action", "Keywords", "SourceBook" };
   }

   private Matcher regxKeywords = Pattern.compile( "✦     (<b>[\\w ]+</b>(?:, <b>[\\w ]+</b>)*)" ).matcher( "" );

   @Override protected void convertEntry ( Entry entry ) {
      entry.meta = new Object[]{ entry.fields[0], entry.fields[1], "", entry.fields[2], "", entry.fields[3] };
      super.convertEntry( entry );
      final String data = entry.data;

      if ( data.startsWith( "<h1 class=dailypower>" ) )
         entry.meta[ FREQUENCY ] = "Daily";
      else if ( data.startsWith( "<h1 class=encounterpower>" ) )
         entry.meta[ FREQUENCY ] = "Encounter";
      else if ( data.startsWith( "<h1 class=atwillpower>" ) )
         entry.meta[ FREQUENCY ] = "At-Will";
      else
         log.log( Level.WARNING, "Power with unknown frequency: {0} {1}", new Object[]{ entry.shortid, entry.name } );

      if ( data.indexOf( '✦' ) >= 0 ) {
         if ( regxKeywords.reset( data ).find() ) {
            Set<String> keywords = new HashSet<>(8); // Some power have multiple keyword lines.
            do {
               keywords.addAll( Arrays.asList( regxKeywords.group( 1 ).replaceAll( "</?b>", "" ).split( ", " ) ) );
            } while ( regxKeywords.find() );
            entry.meta[ KEYWORDS ] = String.join( ", ", keywords.toArray( new String[ keywords.size() ] ) );
         } else {
            // Deathly Glare, Hamadryad Aspects, and Flock Tactics have bullet star but not keywords.
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

   @Override protected void correctEntry(Entry entry) {
      switch ( entry.shortid ) {
         case "power6595": // Bane's Tactics
            entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
            corrections.add( "fix basic attack" );
      }
   }
}