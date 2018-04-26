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
      category.fields = new String[]{ "Tier", "Prerequisite", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxAction    = Pattern.compile( "\\b(Action|Interrupt){1}+</b>\\s*" ).matcher( "" );
   private final Matcher regxPrerequisite = Pattern.compile( "<b>Prerequisite</b>:\\s*+([^<>]++)<" ).matcher( "" );
   private final Matcher regxLevel = Pattern.compile( "(?:, )?+([12]?\\d)(?:st|nd|th){1}+ level(?:, )?+" ).matcher( "" );
   private final Matcher regxAbilityScore = Pattern.compile( "(?i)(Strength|Constitution|Dexterity|Intelligence|Wisdom|Charisma){1}+( \\d++)" ).matcher( "" );

   @Override protected void convertEntry () {
      String oldTier = meta( 0 );
      meta( "Heroic", "", "" );
      super.convertEntry();

      if ( find( regxPrerequisite ) ) {
         String text = regxPrerequisite.group( 1 ).trim();
         if ( text.contains( "-level" ) ) {
            swap( "-level", " level" );
            text = text.replace( "-level", " level" );
            fix( "consistency" );
         }
         if ( text.charAt( text.length() - 1 ) == '.' ) {
            String newText = text.substring( 0, text.length() - 1 );
            swap( text, newText, "consistency" );
            text = newText;
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
            if ( ! oldTier.isEmpty() && ! oldTier.equals( meta( TIER ) ) )
               fix( "wrong meta" );
            else if ( oldTier.isEmpty() && ! meta( TIER ).equals( "Heroic" ) )
               fix( "missing meta" );
         } else if ( text.contains( "level" ) && ! text.contains( "has a level" ) )
            warn( "Feat with unparsed level" );

         if ( ! text.isEmpty() ) {
            text = Character.toLowerCase( text.charAt( 0 ) ) + text.substring( 1 );
            text = text.replace( " class feature", "" );
            text = text.replace( "you have a spellscar", "spellscar" );
            text = text.replace( " you must ", " must " );
            if ( text.contains( " the ") ) {
               if ( text.contains( " must worship" ) )
                  text = text.replace( " of the ", " of " ).replace( " domain", "" ); // a deity of the XX domain >> a deity of XX
               else
                  text = text.replace( " have the ", " have " ).replace( " has the ", " has ").replace( " with the ", " with " );
            }
            text = text.replaceAll( "armor\\s*+(?!of)", "" ); // Turns "hide armor" into "hide", but leave "armor of faith/wrath" alone
            regxAbilityScore.reset( text );
            while ( regxAbilityScore.find() )
               text = text.replace( regxAbilityScore.group(), regxAbilityScore.group( 1 ).substring( 0, 3 ) + regxAbilityScore.group( 2 ) );
            text = Utils.ucfirst( shortenAbility( text ) );
            switch ( entry.getId() ) {
               case "feat2697": // Psionic Complement
               case "feat2698": // Psionic Dabbler
               case "feat2732": // Psionic Conventionalist
                  text = text.replace( " class-specific multiclass feat", " multiclass feat" );
            }
            meta( PREREQUISITE, text );
         }
      }

      // Add feat classification
      if ( find( "[Multiclass" ) ) {
         metaAdd( TIER, " [Multiclass]" );
      } else if ( find( "Style]" ) ) {
         metaAdd( TIER, " [Style]" );
      } else if ( find( "Bloodline]" ) ) {
         metaAdd( TIER, " [Bloodline]" );
      } else if ( find( "[Dragonmark]" ) ) {
         metaAdd( TIER, " [Dragonmark]" );
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
            swap( "basic melee attack", "melee basic attack", "fix basic attack" );
            break;

         case "feat1281": // Bravo Novice
         case "feat1285": // Cutthroat Novice
         case "feat1287": // Cutthroat Specialist
         case "feat1293": // Poisoner Novice
         case "feat1295": // Poisoner Specialist
            locate( regxAction );
            swap( regxAction.group(), regxAction.group( 1 ) + "</b>      <b>Melee or Ranged</b>", "missing keyword" );
            break;

         case "feat2254": // Traveler's Celerity
            swap( "11th-level, ", "11th level, ", "consistency" );
            break;

         case "feat2326": // Arkhosian Fang Student
            swap( "proficiency with the bastard sword, the broadsword, or the greatsword", "proficiency with bastard sword, broadsword, or greatsword", "consistency" );
            break;

         case "feat2698": // Psionic Dabbler
            swap( "class specific", "class-specific", "consistency" );
            break;

         case "feat3667": // Powerful Lure
            swap( "Level 11, ", "11th level, ", "consistency" );
            break;
      }
      super.correctEntry();
   }
}