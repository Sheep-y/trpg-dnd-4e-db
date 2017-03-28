package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class FeatConverter extends Converter {

   private final int TIER = 0;
   private final int PREREQUISITE = 1;

   public FeatConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise () {
      category.meta = new String[]{ "Tier", "Prerequisite", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxPrerequisite = Pattern.compile( "<b>Prerequisite</b>:\\s*([^<>]+)<" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "(?:, )?([12]?\\d)(?:st|nd|th) level(?:, )?" ).matcher( "" );

   @Override protected void convertEntry () {
      String oldTier = meta( 0 );
      meta( "Heroic","", meta( 1 ) );
      super.convertEntry();

      if ( find( regxPrerequisite ) ) {
         String text = regxPrerequisite.group( 1 ).trim();
         if ( text.contains( "-level" ) ) {
            text = text.replace( "-level", " level" );
            fix( "consistency" );
         }
         if ( regxLevel.reset( text ).find() ) {
            int level = Integer.parseInt( regxLevel.group( 1 ) );
            if ( level > 20 )
               meta( TIER, "Epic" );
            else if ( level > 10 )
               meta( TIER, "Paragon" );
            if ( regxLevel.find() )
               warn( "Feat with multiple level" );
            if ( level == 11 || level == 21 )
               text = regxLevel.reset( text ).replaceFirst( "" );
            if ( ! oldTier.isEmpty() && !oldTier.equals( meta( TIER ) ) )
               fix( "wrong meta" );
            else if ( oldTier.isEmpty() && ! meta( TIER ).equals( "Heroic" ) )
               fix( "missing meta" );
         } else if ( text.contains( "level" ) && ! text.contains( "has a level" ) )
            warn( "Feat with unparsed level" );

         if ( ! text.isEmpty() ) {
            text = Character.toLowerCase( text.charAt( 0 ) ) + text.substring( 1 );
            text = text.replace( " class feature", "" );
            text = text.replace( "you have a spellscar", "spellscar" );
            text = text.replace( " have the ", " have " ).replace( " has the ", " has ");
            text = Utils.ucfirst( text );
            meta( PREREQUISITE, text );
         }

      } else if ( find( "rerequi" ) ) {
         warn( "Feat with unparsed prerequisites" );
      }
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int aTier = 0, bTier = 0;
      if ( a.getSimpleField( TIER ).equals( "Paragon" ) ) aTier = 1;
      else if ( a.getSimpleField( TIER ).equals( "Epic" ) ) aTier = 2;
      if ( b.getSimpleField( TIER ).equals( "Paragon" ) ) bTier = 1;
      else if ( b.getSimpleField( TIER ).equals( "Epic" ) ) bTier = 2;

      if ( aTier != bTier ) return aTier - bTier;
      return super.sortEntity( a, b );
   }

   @Override protected void correctEntry () {
      switch ( entry.getId() ) {
         case "feat1111": // Bane's Tactics
            swap( "basic melee attack", "melee basic attack" );
            fix( "fix basic attack" );
            break;

         case "feat2254": // Traveler's Celerity
            swap( "11th-level, ", "11th level, " );
            fix( "consistency" );
            break;

         case "feat3667": // Powerful Lure
            swap( "Level 11, ", "11th level, " );
            fix( "consistency" );
            break;
      }
   }
}