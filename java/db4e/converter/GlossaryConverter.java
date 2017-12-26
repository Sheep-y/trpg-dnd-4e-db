package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossaryConverter extends Converter {

   public GlossaryConverter ( Category category ) {
      super( category );
   }

   private final Matcher regxFlavor = Pattern.compile( "<p class=flavor>(?!.*<p class=flavor>)" ).matcher( "" );

   @Override protected void correctEntry () {
      if ( entry.getId().startsWith( "skill" ) ) { // Fix skills missing "improvising with" title
         if ( ! find( "IMPROVISING WITH" ) ) {
            data( regxFlavor.reset( data() ).replaceFirst( "<h3>IMPROVISING WITH "+entry.getName().toUpperCase()+"</h3><p class=flavor>" ) );
            test( TEXT, "<h3>IMPROVISING WITH " );
            fix( "missing content" );
         }
      }
      switch ( entry.getId() ) {
         case "glossary381": // Concordance, change to Artifact
            entry.setName( "Artifact" );
            swap( "<h1 class=player>Concordance</h1><p class=flavor>", "<h3>Concordance</h3><p>" );
            data( "<h1 class=player>Artifact</h1>"
                    + "<p class=flavor>Artifacts have a level but no price - they can't be " +
"bought or crafted, and their temporary nature ensures that they don't have a long-term impact on a character's " +
"total wealth. As with normal magic items, an artifact's level measures the potency of its properties and " +
"powers, but artifacts break the usual magic item rules.<br><br>" +
"An artifact can't be created, disenchanted, or destroyed by any of the normal means available for " +
"other magic items. In fact, the characters' access to artifacts (and even their retention of recovered artifacts) " +
"is entirely within DM's control. A character can quest after a particular artifact whose existence " +
"is known or suspected, but even then the character acquires an artifact only if the DM says so.<br><br>" +
"Artifact Behavior" +
"Artifacts are sentient - although they're not necessarily communicative - and they have their own motivations. " +
"In many ways, they function like nonplayer characters.<br><br>" +
"An artifact cooperates with its owner as long as doing so fits with the artifact's goals and nature.<br><br>" +
"Each artifact's description contains a list of its goals and roleplaying notes for its personality. Some artifacts " +
"are malevolent and seek to corrupt their wielders, whereas others push the wielder to great acts of heroism.<br><br>" +
"What's more, an artifact's powers change depending on its attitude or connection to its current owner.<br><br>" +
"When its wielder performs actions in concert with its goals, an artifact becomes more powerful, but when " +
"the wielder acts against the artifact's wishes, its power diminishes. The artifact's mindset is measured by a " +
"concordance score.</p>" + data() );
            test( TEXT, "Artifacts have a level but no price" );
            fix( "new entry" );
            break;

         case "glossary664": // Reading a Weapon Entry
            entry.setName( "Weapons" );
            swap( "Reading a Weapon Entry", "Weapons" );
            fix( "consistency" );
            break;

         case "glossary0453": // Item Set
            entry.setFields( new Object[]{ "Rules", "Rules Other", "Adventurer's Vault 2" } ).setContent(
              "<h1 class=player>Item Set</h1>"
               + "<p>A magic item set contains four or more items that a character or a party can collect. " +
"Each set has at least one set benefit that is revealed when a minimum number of the set's items are used together. " +
"Some set items also have individual properties or effects that depend on the number of other set items being used.<br><br>"
               + "A character can benefit from only one individual item set and one group item set at a time. " +
"If a character possesses items from multiple item sets, that character must choose which individual item set and which group item set benefits him or her at the end of each extended rest.<br><br>"
               + "To qualify for an item set's benefits, a character must be wielding or wearing one or more items from the set. " +
"A character that has a weapon or an implement that is part of an item set must be proficient with that weapon or implement to have it qualify as part of an item set. " +
"A stowed item (for example, a magic cloak stuffed in a pack) doesn't count toward a set's benefits (though a sheathed weapon is considered to be worn). " +
"Wondrous items are an exception and need only be carried in order for a character to gain an item set's benefits.<br><br>"
               + "Each magic item in a set can stand alone. " +
"No item needs to be used with another of its set to function.</p>"
               + "<h3>GROUP ITEM SET</h3>"
               + "<p>Some item sets are designed to be borne not by a single character, but by the members of an entire party. " +
"When a party collects the items of a group item set, the set benefits are determined by the number of allies who possess items from the set. " +
"Each character wearing or wielding an item from the set qualifies for the set benefits."
               + "<br><br>Update (08/2012)<br>Insert a paragraph for multiple set and a sentence for proficiency requiremente.<br><br></p>"
               + "<p class=publishedIn>Published in Adventurer's Vault 2, pages 92, 130.</p>" );
            test( TEXT, "A magic item set contains four or more items" );
            fix( "new entry" );
            break;
      }
      super.correctEntry();
   }

   @Override protected Set<String> getLookupName ( Entry entry, Set<String> list ) {
      switch ( entry.getId() ) {
         case "skill27": // Athletics
            return appendList( list, "Athletics", "Escape", "Climb", "Climbing", "Swim", "Swimming", "Jump", "Jumping", "Long Jump" );
         case "glossary86": // Teleportation
            return appendList( list, "Teleport", "Teleportation" );
         case "glossary159": // Hit Points
            return appendList( list, "HP", "Hit Point", "Bloodied" );
         case "glossary163": // Sustained Durations
            return appendList( list, "Sustained Durations", "Sustain", "Sustain Minor", "Sustain Move", "Sustain Standard", "Sustain No Action" );
         case "glossary179": // Defense Scores
            return appendList( list, "Defense Scores", "Defenses", "Defense", "Fortitude", "Reflex", "Will" );
         case "glossary341": // Dying and Death
            return appendList( list, "Dying", "Death", "Death Saving Throw", "Die", "Dies", "Kill", "Drop to 0 or" );
         case "glossary487": // Carrying, Lifting and Dragging
            return appendList( list, "Carry", "Carrying", "Lift", "Lifting", "Drag", "Dragging", "Normal Load", "Heavy Load", "Maximum Drag Load" );
         case "glossary622": // Action Types
            return appendList( list, "Action Type", "Standard Action", "Move Action", "Minor Action", "Immediate Reaction", "Immediate Action", "Immediate Interrupt", "Opportunity Action", "Free Action" );
         case "glossary623": // Languages
            return appendList( list, "Language", "Script" );
         case "glossary670": // Magic Item Level and Rarity
            return appendList( list, "Magic Item Level and Rarity", "Common", "Uncommon", "Rare" );
         case "glossary69": case "glossary659" : case "glossary661" : case "glossary664" : // Implement, Armor, Shields, Weapon
            if ( entry.getId().equals( "glossary69" ) ) list.add( "Implements" );
            appendList( list, "Proficiency", "Proficiencies" ); // Fall through to add singular lookup
      }
      String name = entry.getName();
      if ( name.endsWith( " speed" ) || name.endsWith( " Attack" ) )
         return appendList( list, name, name.substring( 0, name.length() - 6 ) );
      list.add( name );
      if ( name.startsWith( "Object" ) )
         list.add( "Object" );
      else if ( name.toLowerCase().contains( " size" ) )
         list.add( "size" );
      if ( name.endsWith( "s" ) ) {
         list.add( name.substring( 0, name.length() - 1 ) );
         if ( name.endsWith( "es" ) ) {
            list.add( name.substring( 0, name.length() - 2 ) );
            if ( name.endsWith( "ies" ) )
               list.add( name.substring( 0, name.length() - 3 ) + 'y' );
         }
      } else if ( name.endsWith( "ing" ) && ! name.equals( "Sling" ) )
         if ( name.equals( "Dying" ) )
            list.add( "Die" );
         else
            list.add( name.substring( 0, name.length() - 3 ) );
      return list;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( 0 ).compareTo( b.getSimpleField( 0 ) );
      return diff == 0 ? super.sortEntity( a, b ) : diff;
   }
}