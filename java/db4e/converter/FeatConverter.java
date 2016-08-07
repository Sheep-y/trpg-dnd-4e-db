package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class FeatConverter extends Converter {

   private final int TIER = 0;
   private final int PREREQUISITE = 1;

   public FeatConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise () {
      category.meta = new String[]{ "Tier", "Prerequisite", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxPrerequisite = Pattern.compile( "<b>Prerequisite</b>:\\s*([^<>]+)<" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "(?:, )?([12]?\\d)(?:st|nd|th)[- ]level(?:, )?" ).matcher( "" );

   @Override protected void convertEntry ( Entry entry ) {
      entry.meta = new Object[]{ "Heroic","", entry.fields[1] };
      super.convertEntry( entry );
      final String data = entry.data;

      if ( regxPrerequisite.reset( data ).find() ) {

         String text = regxPrerequisite.group( 1 ).trim();
         if ( regxLevel.reset( text ).find() ) {
            int level = Integer.parseInt( regxLevel.group( 1 ) );
            if ( level > 20 )
               entry.meta[ TIER ] = "Epic";
            else if ( level > 10 )
               entry.meta[ TIER ] = "Paragon";
            if ( regxLevel.find() )
               log.log( Level.WARNING, "Feat with multiple level: {0} {1}", new Object[]{ entry.shortid, entry.name } );
            if ( level == 11 || level == 21 )
               text = regxLevel.reset( text ).replaceFirst( "" );
         } else if ( text.contains( "level" ) && ! text.contains( "has a level" ) )
            log.log( Level.WARNING, "Feat with unparsed level: {0} {1}", new Object[]{ entry.shortid, entry.name } );

         if ( ! text.isEmpty() ) {
            text = Character.toLowerCase( text.charAt( 0 ) ) + text.substring( 1 );
            text = text.replace( " class feature", "" );
            text = text.replace( "you have a spellscar", "spellscar" );
            text = text.replace( " have the ", " have " ).replace( " has the ", " has ");
            text = Utils.ucfirst( text );
            entry.meta[ PREREQUISITE ] = text;
         }

      } else if ( data.contains( "rerequi" ) ) {
         log.log( Level.WARNING, "Feat with unparsed prerequisites: {0} {1}", new Object[]{ entry.shortid, entry.name } );
      }
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int aTier = 0, bTier = 0;
      if ( a.meta[TIER].equals( "Paragon" ) ) aTier = 1;
      else if ( a.meta[TIER].equals( "Epic" ) ) aTier = 2;
      if ( b.meta[TIER].equals( "Paragon" ) ) bTier = 1;
      else if ( b.meta[TIER].equals( "Epic" ) ) bTier = 2;

      if ( aTier != bTier ) return aTier - bTier;
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry ( Entry entry ) {
      switch ( entry.shortid ) {
         case "feat1111": // Bane's Tactics
            entry.data = entry.data.replace( "basic melee attack", "melee basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "feat2254": // Traveler's Celerity
            entry.data = entry.data.replace( "11th-level, ", "11th level, " );
            corrections.add( "consistency" );
            break;

         case "feat3667": // Powerful Lure
            entry.data = entry.data.replace( "Level 11, ", "11th level, " );
            corrections.add( "consistency" );
            break;
      }
   }
}