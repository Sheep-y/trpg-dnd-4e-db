package db4e.converter;

import db4e.Main;
import db4e.controller.Controller;
import db4e.data.Category;
import db4e.data.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sheepy.util.Utils;

public class ItemConverter extends LeveledConverter {

   private static final int CATEGORY = 0;
   private static int TYPE;
   private static int COST;
   private static int RARITY;
   private static int SOURCE;
   private final boolean isGeneric;

   public ItemConverter ( Category category ) {
      super( category ); // Sort by category
      isGeneric = category.id.equals( "Item" );
   }

   @Override public void initialise () {
      if ( isGeneric )
         category.fields = new String[]{ "Category", "Type" ,"Level", "Cost", "Rarity", "SourceBook" };
      super.initialise();
      TYPE = LEVEL - 1;
      COST = LEVEL + 1;
      RARITY = COST + 1;
      SOURCE = RARITY + 1;
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      if ( isGeneric ) {
         int diff = a.getSimpleField( CATEGORY ).compareTo( b.getSimpleField( CATEGORY ) );
         if ( diff != 0 ) return diff;
      }
      return super.sortEntity( a, b );
   }

   private final Matcher regxPowerFrequency = Pattern.compile( "✦\\s*+\\(" ).matcher( "" );
   private final Matcher regxWhichIsReproduced = Pattern.compile( " \\([^)]++\\), which is reproduced below(?=.)" ).matcher( "" );
   private final Matcher regxTier = Pattern.compile( "\\b(?:Heroic|Paragon|Epic){1}+\\b" ).matcher( "" );
   private final Matcher regxType = Pattern.compile( "<b>(Type|Armor|Arms Slot|Category){1}+(?:</b>: |: </b>){1}+([A-Za-z, ]++)" ).matcher( "" );
   private final Matcher regxFirstStatBold = Pattern.compile( "<p class=mistat><b>([^<]++)</b>" ).matcher( "" );
   private final Matcher regxPriceTable = Pattern.compile( "<td class=mic1>Lvl (\\d++)(?:<td class=mic2>(?:\\+\\d)?+)?+<td class=mic3>([\\d,]++) gp" ).matcher( "" );

   @Override protected void convertEntry () {
      if ( ! isGeneric )
         entry.setId( entry.getId().replace( "item", category.id.toLowerCase() ) );
      if ( isGeneric && ! meta( RARITY ).equals( "Artifact" ) ) {
         // Artifact fields are already set by Convert.moveArtifact and makeArtifact
         Object[] fields = entry.getFields();
         meta( fields[0], "", fields[1], fields[2], fields[3], fields[4] );
      }
      super.convertEntry();
      if ( meta( RARITY ).equals( "Artifact" ) ) {
         find( regxTier );
         meta( LEVEL, regxTier.group() );
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
            switch ( meta( CATEGORY ) ) {
            case "Alternative Reward" :
               find( regxFirstStatBold );
               meta( TYPE, regxFirstStatBold.group( 1 ) );
               break;
            case "Armor" :
               setArmorType( entry );
               break;
            case "Equipment" :
               if ( find( regxType ) ) {
                  meta( TYPE, regxType.group( 2 ) );
                  if ( meta( TYPE ).equals( "Building" ) ) {
                     meta( CATEGORY, "Lair" );
                     fix( "recategorise" );
                  }
               }
               break;
            case "Item Set" :
               setItemSetType( entry );
               break;
            case "Wondrous" :
               setWondrousType( entry );
         }
      }
      setCost( entry );
   }

   private void setArmorType ( Entry entry ) {
      if ( find( regxType ) ) {
         String type = regxType.group( 2 ).trim();
         // Detect "Chain, cloth, hide, leather, plate or scale" and other variants
         if ( type.split( ", " ).length >= 5 ) {
            data( regxType.replaceFirst( "<b>$1</b>: Any" ) );
            test( TEXT, "</b>: Any" );
            meta( TYPE, "Any" );
            fix( "consistency" );

         } else if ( type.endsWith( "Shields" ) ) {
            meta( TYPE, type.replace( "Shields", "Shield" ) );
            fix( "consistency" );
         } else {
            meta( TYPE, type );
         }
         int minEnhancement = data().indexOf( "<b>Minimum Enhancement Value</b>: " );
         if ( minEnhancement > 0 ) {
            minEnhancement += "<b>Minimum Enhancement Value</b>: ".length();
            meta( LEVEL, "Min " + data().substring( minEnhancement, minEnhancement + 2 ) );
            meta( RARITY, "Masterwork" );
         }

      } else
         switch ( entry.getId() ) {
            case "armor49": case "armor50": case "armor51": case "armor52":
               meta( TYPE, "Barding" );
               break;
            default:
               warn( "Armor type not found" );
         }

      if ( meta( COST ).contains( ".00 gp" ) ) {
         meta( COST, meta( COST ).replace( ".00 ", " " ) );
         fix( "formatting" );
      }
      if ( meta( LEVEL ).isEmpty() ) {
         meta( RARITY, "Mundane" );
         fix( "missing meta" );
      }
   }

   private final Matcher regxImplementType = Pattern.compile( "<b>Implement: </b>([A-Za-z, ]+)" ).matcher( "" );

   private void setImplementType ( Entry entry ) {
      // Magical implements
      if ( find( regxImplementType ) ) {
         meta( TYPE, regxImplementType.group(1).trim() );

      // Superior implements
      } else if ( meta( TYPE ).equals( "Weapon" ) ) {
         meta( TYPE, Utils.ucfirst( entry.getName().replaceFirst( "^\\w+ ", "" ) ) );
         if ( meta( TYPE ).equals( "Symbol" ) ) meta( TYPE, "Holy Symbol" );
         meta( LEVEL, "Superior" );
         meta( RARITY, "Mundane" );
         fix( "recategorise" );

      } else if ( meta( TYPE ).equals( "Equipment" ) ) {
         meta( TYPE, entry.getName().replaceFirst( " Implement$", "" ) );
         meta( RARITY, "Mundane" );
         if ( meta( COST ).isEmpty() ) { // Ki Focus
            meta( COST, "0 gp" );
            fix( "missing meta" );
         }

      } else
         warn( "Implement group not found" );
   }

   private final Matcher regxWeaponDifficulty = Pattern.compile( "\\bSimple|Military|Superior\\b" ).matcher( "" );
   private final Matcher regxWeaponType = Pattern.compile( "<b>Weapon: </b>([A-Za-z, ]+)" ).matcher( "" );
   private final Matcher regxWeaponGroup = Pattern.compile( "<br>([A-Za-z ]+?)(?= \\()" ).matcher( "" );

   private void setWeaponType ( Entry entry ) {
      // Ammunitions does not need processing
      if ( meta( TYPE ).equals( "Ammunition" ) ) return;
      // Mundane weapons with groups
      if ( find( "<b>Group</b>: " ) ) {
         String region = data().substring( data().indexOf( "<b>Group</b>: " ) );
         List<String> grp = Utils.matchAll( regxWeaponGroup, region, 1 );
         if ( grp.isEmpty() )
            warn( "Weapon group not found" );
         else
            meta( TYPE, String.join( ", ", grp ) );
         if ( ! meta( COST ).isEmpty() || entry.getName().endsWith( "secondary end" ) || entry.getName().equals( "Shuriken" ) ) {
            find( regxWeaponDifficulty );
            meta( LEVEL, regxWeaponDifficulty.group() );
         }
         if ( meta( LEVEL ).isEmpty() )
            switch ( entry.getId() ) {
               case "weapon3625": // Unarmed Attacl
                  meta( LEVEL, "Improvised" );
                  break;
               case "weapon3678": // Monk unarmed strike
                  meta( LEVEL, "Simple" );
                  break;
               default:
                  meta( LEVEL, "(Level)" );
                  meta( RARITY, "Pact Weapon" );
                  break;
            }
         if ( meta( RARITY ).isEmpty() && ! entry.getName().toLowerCase().contains( "unarmed" ) )
            meta( RARITY, "Mundane" );
         return;
      }
      // Magical weapons
      if ( find( "<b>Weapon: </b>" ) ) {
         find( regxWeaponType );
         String type = regxWeaponType.group( 1 );
         meta( TYPE, type.equals( "Dragonshard augment" ) ? "Dragonshard" : type );
         return;
      }
      // Manual assign
      switch ( entry.getId() ) {
         case "weapon3677": // Double scimitar - secondary end
            meta( TYPE, "Heavy blade" );
            meta( LEVEL, "Superior" );
            meta( RARITY, "Mundane" );
            break;
         case "weapon3624": case "weapon3626": case "weapon3634": // Improvised weapons
            meta( TYPE, "Improvised" );
            meta( LEVEL, "Improvised" );
            break;
         case "weapon176": case "weapon180": case "weapon181": case "weapon219": case "weapon220": case "weapon221": case "weapon222": case "weapon259": // Arrows, magazine, etc.
            meta( TYPE, "Ammunition" );
            meta( RARITY, "Mundane" );
            break;
         default:
            warn( "Unknown weapon type" );
      }
   }

   private void setItemSetType ( Entry entry ) {
      String type = "";
      switch ( entry.getId() ) {
         case "item425": // Mirror of Nessecar
            type = "Arcane"; break;
         case "item429": // Tinkerer's Inventions
            type = "Artificer"; break;
         case "item439": // Xenda-Dran’s Array
            type = "Assassin";
            meta( LEVEL, "Heroic" );
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
            type = "Group, Fey"; break;
         case "item432": // Fortune Stones
            type = "Group, Reroll"; break;
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
            warn( "Unknown item set" );
      }
      meta( TYPE, type );
   }

   private void setWondrousType ( Entry entry ) {
      if ( entry.getName().contains( "Tattoo" ) )
         meta( TYPE, "Tattoo" );
      else if ( find( "implement for bard powers" ) )
         meta( TYPE, "Instruments" );
      else if ( find( "as an implement" ) )
         meta( TYPE, "Implement" );
      else if ( find( "primordial shard" ) )
         meta( TYPE, "Primordial Shard" );
      else if ( find( "Conjuration" ) && find( "figurine" ) )
         meta( TYPE, "Figurine" );
      else if ( find( "standard" ) && find( "plant th" ) )
         meta( TYPE, "Standard" );
      else if ( find( "dim light" ) || find( "bright light of" ) )
         meta( TYPE, "Lighting" );
      if ( find( "Conjuration" ) && find( "mount" ) && ! entry.getName().startsWith( "Bag " ) )
         if ( meta( 1 ).isEmpty() )
            meta( TYPE, "Mount" );
         else
            meta( TYPE, meta( TYPE ) + ": Mount" );
   }

   private final List<Object> multi_cost = new ArrayList<>();
   private final List<Object> multi_level = new ArrayList<>();

   /**
    * Parse cost and level table to output multiple meta
    * @param entry
    */
   private void setCost ( Entry entry ) {
      if ( ! meta( LEVEL ).endsWith( "+" ) ) {
         if ( ! Main.debug.get() ) return;
         if ( TYPE > 0 && meta( TYPE-1 ).equals( "Item Set" ) ) return;
         if ( Main.debug.get() && find( regxPriceTable ) && regxPriceTable.find() )
            warn( "Price table on non-multilevel item" );
         return;
      }
      if ( ! find( regxPriceTable ) ) {
         warn( "Price table not found on multilevel item" );
         return;
      }
      multi_cost.clear();
      multi_level.clear();
      multi_cost.add( meta( COST ) );
      multi_level.add( meta( LEVEL ) );
      do {
         multi_cost.add( regxPriceTable.group( 2 ).replaceAll( "\\D", "" ) );
         multi_level.add( regxPriceTable.group( 1 ) );
      } while ( regxPriceTable.find() );
      meta( COST , multi_cost .toArray() );
      meta( LEVEL, multi_level.toArray() );
   }

   @Override protected void correctEntry () {
      if ( ! find( regxPublished ) ) {
         data( data() + "<p class=publishedIn>Published in " + meta( SOURCE )  + ".</p>" );
         test( TEXT, "<p class=publishedIn>Published in" );
         fix( "missing published" );
      }

      if ( find( ", which is reproduced below." ) ) {
         data( regxWhichIsReproduced.reset( data() ).replaceFirst( "" ) );
         test( TEXT, Pattern.compile( ".*(?!, which is reproduced below\\.)" ) );
         fix( "consistency" );
      }

      if ( find( "this item cannot be purchased or created with the Enchant Magic Item ritual." ) ) {
         meta( RARITY, "Unique" );
         fix( "wrong meta" );
      }

      if ( meta( CATEGORY ).equals( "Alchemical Item" ) ) {
         meta( CATEGORY, "Consumable" );
         meta( TYPE, "Alchemical Item" );
         fix( "recategorise" );
      }

      switch ( entry.getId() ) {
         case "armor1": // Cloth Armor
            meta( COST, "1 gp" );
            fix( "wrong meta" );
            break;

         case "armor105": // Shield of Prator
            swap( " class=magicitem>", " class=mihead>" );
            fix( "title colour" );
            break;

         case "item434": // Rings of the Akarot
            swapFirst( "<br><br>",
                         "<br><br><h1 class=dailypower>Voice of the Akarot<span class=level>Item Set Power</span></h1>"
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
            fix( "content" );
            break;

         case "item439": // Xenda-Dran's Array
            swap( "> Tier</", "> Heroic Tier</" );
            fix( "consistency" );
            break;

         case "item467": // Alchemical Failsafe
            swap( "Power ✦ </h2>", "Power ✦ At-Will</h2>" );
            fix( "missing power frequency" );
            break;

         case "item508": // Anarch Sphere
         case "item1578": // Horreb Ritual Cube
         case "item2463": // Shard of Evil
            swap( "0 gp", "Priceless" );
            meta( COST, "" );
            fix( "consistency" );
            break;

         case "implement509":  // Anarusi Codex
            swap( "0 gp", "5,000 gp" );
            meta( COST, "5,000 gp" );
            fix( "content" );
            break;

         case "item588":  // Bahamut's Golden Canary
            swap( "0 gp", "Priceless" );
            meta( COST, "" );
            fix( "consistency" );
            // fall through
         case "item1632": // Instant Portal
            meta( CATEGORY, "Consumable" );
            fix( "recategorise" );
            break;

         case "item1007": // Dantrag's Bracers, first (arm) power is daily, second (feet) power is encounter
            swapFirst( "Power ✦ </h2>", "Power ✦ Daily</h2>" );
            swapFirst( "Power ✦ </h2>", "Power ✦ Encounter</h2>" );
            fix( "missing power frequency" );
            break;

         case "weapon1006": // Dancing Weapon
         case "armor1261": // Feral Armor
         case "weapon2451": // Shadowfell Blade
            swap( "basic melee attack", "melee basic attack" );
            fix( "fix basic attack" );
            break;

         case "item1701": // Kord's Relentlessness
            swap( " or 30:</i> Gain a +2 item bonus to death</p>",
                  " or 20</i>: +4 item bonus to the damage roll<br>    <i>Level 25 or 30:</i> +6 item bonus to the damage roll</p>" );
            fix( "content" );
            break;

         case "item1895": // Mrtok, Ogre Chief (Gauntlets of Ogre Power)
            swap( " 0 gp", " 1,000 gp" );
            meta( COST, "1,000 gp" );
            fix( "content" );
            break;

         case "implement2002": // Orium Implement
            swap( "<b>Implement</b>", "<b>Implement: </b>Orb, Rod, Staff, Wand" );
            swap( "<p class='mistat indent'><b>Requirement:</b> Orb, Rod, Staff, Wand</p>", "" );
            fix( "content" );
            break;

         case "weapon2495": // Shivli, White Wyrmling (Frost Weapon)
            swap( ">+2<td class=mic3>0 gp<", ">+2<td class=mic3>3,400 gp<" );
            swap( ">+3<td class=mic3>0 gp<", ">+3<td class=mic3>17,000 gp<" );
            swap( ">+4<td class=mic3>0 gp<", ">+4<td class=mic3>85,000 gp<" );
            swap( ">+5<td class=mic3>0 gp<", ">+5<td class=mic3>425,000 gp<" );
            swap( ">+6<td class=mic3>0 gp<", ">+6<td class=mic3>2,125,000 gp<" );
            meta( COST, "3,400+ gp" );
            fix( "content" );
            break;

         case "item2511": // Silver Hands of Power
            swap( "<h2 class=mihead>Power", "<h2 class=mihead>Lvl 14<br>Power" );
            swap( "<p class='mistat indent1'><i>Level 19:</i> ", "<h2 class=mihead>Lvl 19<br>Power ✦ Daily (Free Action)</h2>" );
            swap( "Trigger: You", "<p class='mistat indent1'><i>Trigger:</i> You" );
            swap( ". Effect: ", "</p><p class='mistat indent1'><i>Effect:</i> " );
            fix( "formatting" );
            break;

         case "item2971": // Vecna's Boon of Diabolical Choice
            swap( "Level 0 Uncommon", "Level 24 Uncommon" );
            meta( LEVEL, "24" );
            fix( "content" );
            // fall through
         case "item1806": // Mark of the Star
         case "item2469": // Shelter of Fate
         case "item2533": // Slaying Stone of Kiris Dahn
         case "item2995": // Vision of the Vizier
            swap( " 0 gp", "" );
            meta( COST, "" );
            fix( "consistency" );
            break;

         case "weapon3328": // Scepter of the Chosen Tyrant
            swap( "basic ranged attack", "ranged basic attack" );
            fix( "fix basic attack" );
            break;

         case "item3331": // Sun's Sliver
            swap( ">Level <", ">Epic Tier<" );
            swap( "<b>Wondrous Item</b>", "<b>Minor Artifact:</b> Wondrous Item" );
            meta( RARITY, "Artifact" );
            meta( COST, "" );
            fix( "content" );
            break;

         case "weapon3415": // The Fifth Sword of Tyr
            swap( "Power (Teleportation) ✦ Daily", "Power (Weapon) ✦ Daily" );
            fix( "typo" );
            break;

         case "item3545" : // Mold King's Crown
            swap( "</i><br></p>", "</i></p>" ); // Does not affect layout so not count as a fix
            break;

         case "item1864": // Mirror of Deception
            swap( " ✦ (Standard", " ✦ At-Will (Standard" );
            swap( "alter</p><p class='mistat indent'>sound", "alter sound" );
            fix( "missing power frequency" );
            fix( "formatting" );
            // fallthrough

         case "item475": // Alchemist's Workshop
         case "item479": // All-Seeing Eye
         case "item524": // Arcane Laboratory
         case "item568": // Austere Dojo
         case "item635": // Bed of Rapid Rest
         case "item815": // Brilliant Scrying Basin
         case "item837": // Cask of Liquid Gold
         case "item856": // Chandelier of Revelation
         case "item1047": // Deceptive Scrying Basin
         case "item1087": // Dimensional Anchor
         case "item1092": // Diplomat's Table
         case "item1106": // Door of Alarmr
         case "item1105": // Door Warden
         case "item1256": // Feast Table
         case "item1458": // Gorgonblood Mortar
         case "item1563": // Holy Shrine
         case "item1748": // Loadstone Statue
         case "item1773": // Magic Drawbridge
         case "item1783": // Magic Weapon Rack
         // case "item1864": // Mirror of Deception (fall through from above)
         case "item2271": // Ritualist's Lectern
         case "item2371": // Sacred Glade
         case "item2490": // Shiftstone
         case "item2493": // Shining Sundial
         case "item2507": // Silence-Warded Room
         case "item2593": // Spying Mirrors (pair)
         case "item2720": // Sun Globe
         case "item2840": // Teleportation Disk
         case "item2873": // Throne of Dominion
         case "item2874": // Throne of Grandeur
         case "item2991": // Vigilant Gargoyle
         case "item3047": // Watchful Eye
         case "item3078": // Window of Deception
         case "item3079": // Window of Escape
            meta( CATEGORY, "Lair" );
            fix( "recategorise" );
            // fall through

         default:
            // Add "At-Will" to the ~150 items missing a power frequency.
            // I checked each one and the exceptions are above.
            if ( find( regxPowerFrequency ) ) {
               data( regxPowerFrequency.replaceAll( "✦ At-Will (" ) );
               test( TEXT, "✦ At-Will (" );
               fix( "missing power frequency" );
            }
      }
      super.correctEntry();
   }

   @Override public String textData( String data ) {
      if ( data.startsWith( "<h1 class=miset>" ) && Controller.fixData )
         data = data.replaceFirst( "<h1 class=mihead>.*(?=<p class=publishedIn>)", "" );
      return super.textData( data );
   }

   @Override protected Set<String> getLookupName ( Entry entry, Set<String> list ) {
      switch ( category.id ) {
         case "Implement" :
            String name = entry.getName();
            if ( name.endsWith( " Implement" ) ) name = name.substring( 0, name.length()-10 ); // Basic implements
            return appendList( list, regxNote.reset( name ).replaceAll( "" ).trim() );
         case "Item" :
            switch ( entry.getId() ) {
               case "item171": // Belt Pouch (empty)
                  return appendList( list, "Belt Pouch", "Pouch" );
            }
         case "Armor" :
            switch ( entry.getId() ) {
               case "armor1": // Cloth
                  return appendList( list,  "Cloth Armor", "Cloth", "Clothing" );
               case "armor2": case "armor3": case "armor5": case "armor6": // Leather to Plate
                  return appendList( list, entry.getName(), entry.getName().replace( " Armor", "" ) );
               case "armor4": // Chainmail
                  return appendList( list, entry.getName(), "Chain" );
               case "armor7": case "armor8": // Light/Heavy shield
                  return appendList( list, entry.getName(), "Shields", "Shield" );
               case "armor49": case "armor51": // Barding (Normal)
                  return appendList( list, entry.getName(), "Barding", "Bardings" );
            }
      }
      return super.getLookupName( entry, list );
   }
}