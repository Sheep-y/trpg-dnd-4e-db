package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class ItemConverter extends LeveledConverter {

   private static final int CATEGORY = 0;
   private static int COST;
   private final boolean isGeneric;

   public ItemConverter ( Category category, boolean debug ) {
      super( category, debug ); // Sort by category
      isGeneric = category.id.equals( "Item" );
   }

   @Override public void initialise () {
      if ( isGeneric )
         category.meta = new String[]{ "Category", "Type" ,"Level", "Cost", "Rarity", "SourceBook" };
      super.initialise();
      COST = LEVEL + 1;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      if ( isGeneric ) {
         int diff = a.meta[ CATEGORY ].toString().compareTo( b.meta[ 0 ].toString() );
         if ( diff != 0 ) return diff;
      }
      return super.sortEntity( a, b );
   }

   private final Matcher regxPowerFrequency = Pattern.compile( "✦\\s*\\(" ).matcher( "" );
   private final Matcher regxWhichIsReproduced = Pattern.compile( " \\([^)]+\\), which is reproduced below(?=.)" ).matcher( "" );
   private final Matcher regxTier = Pattern.compile( "\\b(?:Heroic|Paragon|Epic)\\b" ).matcher( "" );
   private final Matcher regxType = Pattern.compile( "<b>(?:Type|Armor|Arms Slot|Category)(?:</b>: |: </b>)([A-Za-z, ]+)" ).matcher( "" );
   private final Matcher regxFirstStatBold = Pattern.compile( "<p class=mistat><b>([^<]+)</b>" ).matcher( "" );

   @Override protected void convertEntry ( Entry entry ) {
      if ( isGeneric ) {
         String[] fields = entry.fields;
         if ( entry.meta == null ) // Fix level field position before sorting
            entry.meta = new Object[]{ fields[0], "", fields[1], fields[2], fields[3], fields[4] };
      }
      super.convertEntry( entry );
      if ( ! isGeneric )
         entry.shortid = entry.shortid.replace( "item", category.id.toLowerCase() );
      if ( ( ! isGeneric && entry.meta[0].toString().startsWith( "Artifact" ) ) ||
             ( isGeneric && entry.meta[1].toString().startsWith( "Artifact" ) ) ) {
         regxTier.reset( entry.data ).find();
         entry.meta[ isGeneric ? 2 : 1 ] = regxTier.group();
         return; // Artifacts already have its type set
      }
      // Group Items
      switch ( category.id ) {
         case "Implement" :
            setImplementType( entry ); // Implements's original category may be "Equipment", "Weapon", or "Implement".
            break;
         case "Armor" :
            setArmorType( entry ); // Armor's original category  may be "Armor" or "Arms"
            break;
         case "Weapon" :
            setWeaponType( entry ); // Weapon's original category  may be "Weapon" or "Equipment"
            break;
         default:
            switch ( entry.meta[0].toString() ) {
            case "Alternative Reward" :
               regxFirstStatBold.reset( entry.data ).find();
               entry.meta[1] = regxFirstStatBold.group( 1 );
               break;
            case "Armor" :
               setArmorType( entry );
               break;
            case "Equipment" :
               if ( regxType.reset( entry.data ).find() )
                  entry.meta[1] = regxType.group( 1 );
               break;
            case "Item Set" :
               setItemSetType( entry );
               break;
            case "Wondrous" :
               setWondrousType( entry );
         }
      }
   }

   private void setArmorType ( Entry entry ) {
      if ( regxType.reset( entry.data ).find() ) {
         entry.meta[0] = regxType.group( 1 ).trim();
         // Detect "Chain, cloth, hide, leather, plate or scale" and other variants
         if ( entry.meta[0].toString().split( ", " ).length >= 5 ) {
            entry.data = regxType.replaceFirst( "<b>$1</b>: Any" );
            entry.meta[0] = "Any";
            corrections.add( "consistency" );
         }
         int minEnhancement = entry.data.indexOf( "<b>Minimum Enhancement Value</b>: " );
         if ( minEnhancement > 0 ) {
            minEnhancement += "<b>Minimum Enhancement Value</b>: ".length();
            entry.meta[1] = "Min " + entry.data.substring( minEnhancement, minEnhancement + 2 );
         }

      } else
         switch ( entry.shortid ) {
            case "armor49": case "armor50": case "armor51": case "armor52":
               entry.meta[0] = "Barding";
               break;
            default:
               log.log( Level.WARNING, "Armor type not found: {0} {1}", new Object[]{ entry.shortid, entry.name} );
         }
   }

   private final Matcher regxImplementType = Pattern.compile( "<b>Implement: </b>([A-Za-z, ]+)" ).matcher( "" );

   private void setImplementType ( Entry entry ) {
      // Magical implements
      if ( regxImplementType.reset( entry.data ).find() ) {
         entry.meta[0] = regxImplementType.group(1).trim();

      // Superior implements
      } else if ( entry.meta[0].equals( "Weapon" ) ) {
         entry.meta[0] = Utils.ucfirst( entry.name.replaceFirst( "^\\w+ ", "" ) );
         if ( entry.meta[0].equals( "Symbol" ) ) entry.meta[0] = "Holy Symbol";
         entry.meta[1] = "Superior";
         corrections.add( "recategorise" );

      } else if ( entry.meta[0].equals( "Equipment" ) ) {
         entry.meta[0] = entry.name.replaceFirst( " Implement$", "" );
         entry.meta[1] = "Mundane";

      } else
         log.log( Level.WARNING, "Implement group not found: {0} {1}", new Object[]{ entry.shortid, entry.name} );
   }

   private final Matcher regxWeaponDifficulty = Pattern.compile( "\\bSimple|Military|Superior\\b" ).matcher( "" );
   private final Matcher regxWeaponType = Pattern.compile( "<b>Weapon: </b>([A-Za-z, ]+)" ).matcher( "" );
   private final Matcher regxWeaponGroup = Pattern.compile( "<br>([A-Za-z ]+?)(?= \\()" ).matcher( "" );

   private void setWeaponType ( Entry entry ) {
      String data = entry.data;
      // Ammunitions does not need processing
      if ( entry.meta[0].equals( "Ammunition" ) ) return;
      // Mundane weapons with groups
      if ( data.contains( "<b>Group</b>: " ) ) {
         int groupPos = data.indexOf( "<b>Group</b>: " );
         String region = data.substring( groupPos );
         List<String> grp = Utils.matchAll( regxWeaponGroup, region, 1 );
         if ( grp.isEmpty() )
            log.log( Level.WARNING, "Weapon group not found: {0} {1}", new Object[]{ entry.shortid, entry.name} );
         else
            entry.meta[ 0 ] = String.join( ", ", grp );
         if ( ! entry.meta[2].equals( "" ) || entry.name.endsWith( "secondary end" ) || entry.name.equals( "Shuriken" ) ) {
            regxWeaponDifficulty.reset( entry.data ).find();
            entry.meta[ 1 ] = regxWeaponDifficulty.group();
         }
         if ( entry.meta[ 1 ].toString().isEmpty() )
            entry.meta[ 1 ] = entry.meta[ 0 ].equals( "Unarmed" ) ? "Improvised" : "(Level)";
         return;
      }
      // Magical weapons
      if ( data.contains( "<b>Weapon: </b>" ) ) {
         regxWeaponType.reset( data ).find();
         entry.meta[0] = regxWeaponType.group( 1 );
         if ( entry.meta[0].equals( "Dragonshard augment" ) )
            entry.meta[0] = "Dragonshard"; // shorten type
         return;
      }
      // Manual assign
      switch ( entry.shortid ) {
         case "weapon3677": // Double scimitar - secondary end
            entry.meta[ 0 ] = "Heavy blade";
            entry.meta[ 1 ] = "Superior";
            break;
         case "weapon3624": case "weapon3626": case "weapon3634": // Improvised weapons
            entry.meta[ 0 ] = "Improvised";
            entry.meta[ 1 ] = "Improvised";
            break;
         case "weapon176": case "weapon180": case "weapon181": case "weapon219": case "weapon220": case "weapon221": case "weapon222": case "weapon259": // Arrows, magazine, etc.
            entry.meta[ 0 ] = "Ammunition";
            entry.meta[ 1 ] = "Mundane";
            break;
         default:
            log.log( Level.WARNING, "Unknown weapon type: {0} {1}", new Object[]{ entry.shortid, entry.name} );
      }
   }

   private void setItemSetType ( Entry entry ) {
      String type = "";
      switch ( entry.shortid ) {
         case "item425": // Mirror of Nessecar
            type = "Arcane"; break;
         case "item429": // Tinkerer's Inventions
            type = "Artificer"; break;
         case "item439": // Xenda-Dran’s Array
            type = "Assassin";
            entry.meta[2] = "Heroic";
            break;
         case "item406": // Radiant Temple Treasures
            type = "Avenger"; break;
         case "item403": // Golden Lion's Battle Regalia
            type = "Barbarian"; break;
         case "item415": // Champion's Flame
            type = "Cleric"; break;
         case "item413": // Aspect of the Ram
            type = "Charge"; break;
         case "item404": // Kamestiri Uniform
            type = "Crossbow"; break;
         case "item414": // Ayrkashna Armor
            type = "Deva"; break;
         case "item399": // Aleheart Companions' Gear
         case "item419": // Panoply of the Shepherds of Ghest
         case "item421": // Raiment of the World Spirit
            type = "Defense"; break;
         case "item424": // Relics of the Forgotten One
            type = "Divine"; break;
         case "item436": // Silver Dragon Regalia
            type = "Dragonborn"; break;
         case "item409": // Skin of the Panther
            type = "Druid"; break;
         case "item402": // Gadgeteer's Garb
            type = "Gadget"; break;
         case "item430": // Armory of the Unvanquished
         case "item433": // Heirlooms of Mazgorax
         case "item435": // Implements of Argent
         case "item434": // Rings of the Akarot
         case "item438": // The Returning Beast
            type = "Group"; break;
         case "item431": // Caelynnvala's Boons
            type = "Group: Fey"; break;
         case "item432": // Fortune Stones
            type = "Group: Reroll"; break;
         case "item407": // Resplendent Finery
            type = "Illusion"; break;
         case "item427": // Relics of Creation
            type = "Invoker"; break;
         case "item422": // Reaper's Array
            type = "Offense"; break;
         case "item400": // Arms of War
            type = "Opportunity"; break;
         case "item417": // Gifts for the Queen
            type = "Lightning/Radiant"; break;
         case "item412": // Arms of Unbreakable Honor
            type = "Paladin"; break;
         case "item401": // Blade Dancer's Regalia
            type = "Ranger"; break;
         case "item410": // Tools of Zane's Vengeance
            type = "Shaman"; break;
         case "item418": // Offerings of Celestian
            type = "Sorcerer"; break;
         case "item408": // Shadowdancer's Garb
            type = "Stealth"; break;
         case "item416": // Eldritch Panoply
         case "item405": // Marjam's Dream
            type = "Swordmage"; break;
         case "item437": // Royal Regalia of Chessenta
            type = "Tiamat"; break;
         case "item428": // Time Wizard's Tools
            type = "Time"; break;
         case "item420": // Raiment of Shadows
         case "item426": // Points of the Constellation
         case "item411": // Zy Tormtor's Trinkets
            type = "Warlock"; break;
         case "item423": // Regalia of the Golden General
            type = "Warlord"; break;
         default:
            log.log( Level.WARNING, "Unknown item set: {0} {1}", new Object[]{ entry.shortid, entry.name} );
      }
      entry.meta[1] = type;
   }

   private void setWondrousType ( Entry entry ) {
      String data = entry.data;
      Object[] meta = entry.meta;
      if ( entry.name.contains( "Tattoo" ) )
         meta[1] = "Tattoo";
      else if ( data.contains( "primordial shard" ) )
         meta[1] = "Primordial Shard";
      else if ( data.contains( "Conjuration" ) && data.contains( "figurine" ) )
         meta[1] = "Figurine";
      else if ( data.contains( "standard" ) && data.contains( "plant th" ) )
         meta[0] = "Standard";
      if ( data.contains( "Conjuration" ) && data.contains( "mount" ) && ! entry.name.startsWith( "Bag " ) )
         if ( meta[1].toString().isEmpty() )
            meta[1] = "Mount";
         else
            meta[1] = meta[1] + ": Mount";
   }

   @Override protected void correctEntry ( Entry entry ) {
      if ( ! regxPublished.reset( entry.data ).find() ) {
         entry.data += "<p class=publishedIn>Published in " + entry.meta[ 4 ]  + ".</p>";
         corrections.add( "missing published" );
      }

      if ( entry.data.contains( ", which is reproduced below." ) ) {
         entry.data = regxWhichIsReproduced.reset( entry.data ).replaceFirst( "" );
         corrections.add( "consistency" );
      }

      String data = entry.data;
      switch ( entry.shortid ) {
         case "item105": // Shield of Prator
            entry.data = data.replace( " class=magicitem>", " class=mihead>" );
            corrections.add( "formatting" );
            break;

         case "item434": // Rings of the Akarot
            entry.data = data.replaceFirst( "<br><br>",
                         "<br><br><h1 class=dailypower><span class=level>Item Set Power</span>Voice of the Akarot</h1>"
                       + "<p class=flavor><i>Channeling the power of your allies' will, you command your enemy to stop attacking, though each ally is momentarily disoriented.</i></p>"
                       + "<p class=powerstat><b>Daily (Special)</b> ✦     <b>Charm</b><br>"
                       + "<b>Standard Action</b>      <b>Close</b> burst 5</p>"
                       + "<p class=powerstat><b>Target</b>: Each enemyin burst</p>"
                       + "<p class=powerstat><b>Attack</b>: +30 vs. Will</p>"
                       + "<p class=flavor><b>Hit</b>: The target cannot attack (save ends).</p>"
                       + "<p class=powerstat><b>Effect</b>: Each ally wearing a ring from this set is dazed until the end of your next turn.</p>"
                       + "<p class=flavor><b>Special</b>: This power can be used only once per day by you and your allies. Once any of you use it, "
                       + "the group does not regain the use of the power until the person who used it takes an extended rest.</p>"
                       + "<br>Update (4/12/2010)<br> In the Keywords entry, add \"(Special)\" after \"Daily.\" In addition, add the Special entry to the power. "
                       + "These changes limit the potential for this power to shut down multiple encounters.<br><br>" );
            corrections.add( "missing content" );
            break;

         case "item439": // Xenda-Dran's Array
            entry.data = data.replace( "> Tier</", "> Heroic Tier</" );
            corrections.add( "consistency" );
            break;

         case "item467": // Alchemical Failsafe
            entry.data = data.replace( "Power ✦ </h2>", "Power ✦ At-Will</h2>" );
            corrections.add( "missing power frequency" );
            break;

         case "item588":  // Bahamut's Golden Canary
            entry.data = entry.data.replace( "0 gp", "priceless" );
            entry.meta[ COST ] = "";
            corrections.add( "consistency" );
            // fall through
         case "item1632": // Instant Portal
            entry.meta[0] = "Consumable";
            corrections.add( "recategorise" );

         case "item1007": // Dantrag's Bracers, first (arm) power is daily, second (feet) power is encounter
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Daily</h2>" );
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Encounter</h2>" );
            corrections.add( "missing power frequency" );
            break;

         case "item1006": // Dancing Weapon
         case "item1261": // Feral Armor
         case "item2451": // Shadowfell Blade
            entry.data = data.replace( "basic melee attack", "melee basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "item1701": // Kord's Relentlessness
            entry.data = data.replace( " or 30:</i> Gain a +2 item bonus to death</p>",
                  " or 20</i>: +4 item bonus to the damage roll<br>    <i>Level 25 or 30:</i> +6 item bonus to the damage roll</p>" );
            corrections.add( "missing content" );
            break;

         case "item1864": // Mirror of Deception
            entry.data = entry.data.replace( " ✦ (Standard", " ✦ At-Will (Standard" );
            entry.data = entry.data.replace( "alter</p><p class='mistat indent'>sound", "alter sound" );
            corrections.add( "missing power frequency" );
            corrections.add( "formatting" );
            break;

         case "item1895": // Mrtok, Ogre Chief (Gauntlets of Ogre Power)
            entry.data = entry.data.replace( " 0 gp", " 1,000 gp" );
            entry.meta[ COST ] = "1,000 gp";
            corrections.add( "consistency" );
            break;

         case "item2002": // Orium Implement
            entry.data = entry.data.replace( "<b>Implement</b>", "<b>Implement: </b>Orb, Rod, Staff, Wand" );
            entry.data = entry.data.replace( "<p class='mistat indent'><b>Requirement:</b> Orb, Rod, Staff, Wand</p>", "" );
            corrections.add( "missing content" );
            break;

         case "item2495": // Shivli, White Wyrmling (Frost Weapon)
            entry.data = entry.data.replace( ">+2</td><td class=mic3>0 gp<", ">+2</td><td class=mic3>3,400 gp<" );
            entry.data = entry.data.replace( ">+3</td><td class=mic3>0 gp<", ">+3</td><td class=mic3>17,000 gp<" );
            entry.data = entry.data.replace( ">+4</td><td class=mic3>0 gp<", ">+4</td><td class=mic3>85,000 gp<" );
            entry.data = entry.data.replace( ">+5</td><td class=mic3>0 gp<", ">+5</td><td class=mic3>425,000 gp<" );
            entry.data = entry.data.replace( ">+6</td><td class=mic3>0 gp<", ">+6</td><td class=mic3>2,125,000 gp<" );
            entry.meta[ COST ] = "3,400+ gp";
            corrections.add( "consistency" );
            break;

         case "item2511": // Silver Hands of Power
            entry.data = entry.data.replace( "<h2 class=mihead>Power", "<h2 class=mihead>Lvl 14<br>Power" );
            entry.data = entry.data.replace( "<p class='mistat indent1'><i>Level 19:</i> ", "<h2 class=mihead>Lvl 19<br>Power ✦ Daily (Free Action)</h2>" );
            entry.data = entry.data.replace( "Trigger: You", "<p class='mistat indent1'><i>Trigger:</i> You" );
            entry.data = entry.data.replace( ". Effect: ", "</p><p class='mistat indent1'><i>Effect:</i> " );
            corrections.add( "formatting" );
            break;

         case "item2971": // Vecna's Boon of Diabolical Choice
            entry.data = entry.data.replace( "Level 0 Uncommon", "Level 24 Uncommon" );
            entry.meta[ LEVEL ] = 24;
            corrections.add( "missing content" );
            // fall through
         case "item1806": // Mark of the Star
         case "item2469": // Shelter of Fate
         case "item2995": // Vision of the Vizier
            entry.data = entry.data.replace( "        0 gp", "" );
            entry.meta[ COST ] = "";
            corrections.add( "consistency" );
            break;

         case "item3328": // Scepter of the Chosen Tyrant
            entry.data = data.replace( "basic ranged attack", "ranged basic attack" );
            corrections.add( "fix basic attack" );
            break;

         case "item3415": // The Fifth Sword of Tyr
            entry.data = data.replace( "Power (Teleportation) ✦ Daily", "Power (Weapon) ✦ Daily" );
            corrections.add( "typo" );
            break;

         default:
            // Add "At-Will" to the ~150 items missing a power frequency.
            // I checked each one and the exceptions are above.
            if ( regxPowerFrequency.reset( data ).find() ) {
               entry.data = regxPowerFrequency.replaceAll( "✦ At-Will (" );
               corrections.add( "missing power frequency" );
            }
      }
   }

   @Override protected String[] getLookupName ( Entry entry ) {
      switch ( category.id ) {
         case "Implement" :
            String name = entry.name;
            if ( name.endsWith( " Implement" ) ) name = name.substring( 0, name.length()-10 ); // Basic implements
            return new String[]{ regxNote.reset( name ).replaceAll( "" ).trim() };
         case "Armor" :
            switch ( entry.shortid ) {
               case "armor1": case "armor2": case "armor3": case "armor5": case "armor6": // Cloth to Plate
                  return new String[]{ entry.name, entry.name.replace( " Armor", "" ) };
               case "armor4": // Chainmail
                  return new String[]{ entry.name, "Chain" };
               case "armor7": case "armor8": // Light/Heavy shield
                  return new String[]{ entry.name, "Shields", "Shield" };
               case "armor49": case "armor51": // Barding (Normal)
                  return new String[]{ entry.name, "Barding", "Bardings" };
            }
            // Fall through
         default:
            return super.getLookupName( entry );
      }
   }
}