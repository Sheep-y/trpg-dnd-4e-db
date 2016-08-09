# 4e Compendium Downloader #

![Screenshot of program](https://raw.githubusercontent.com/Sheep-y/trpg-dnd-4e-db/master/res/img/History%20-%20v3.5%20viewer.png)

Version 3.5.2 development branch

This app can be used to reterives and locally stores entries from online 4e [D&D Compendium](http://www.wizards.com/dndinsider/compendium/database.aspx).
 <br/>
Stored entries can be exported, then browsed and searched with exact phase search, wildcast, exclusion, join ("OR"), and even regular expression.

This is a fan-made project and does not come with copyrighted data.

## How To Download Data ##

1. You need an active [Dungeons & Dragons Insider subscription](http://ddi.wizards.com/) to retrieve data.
2. [Download](http://www.java.com/) and install Java (version 8 or above).
3. [Download](https://github.com/Sheep-y/trpg-dnd-4e-db/releases/) the downloader exe (Windows) or downloader jar (Linux/Mac).
4. Open a folder for the downloader, put it in, and run it.
   5. Jar version: If double clicking the jar file does not work, open console and run "java -jar 4e_compendium_downloader.jar". Note that the downloader is a GUI program.
6. In the downloader, fill in DDI username and password, then click "Download".
   7. Download can be stopped and resumed any time.
   8. See in-downloader help for details and troubleshoots.
9. Once all data is downloaded, you can export the data to an HTML file, which can be opened in browsers.

### Fixing compendium errors ###

Let's face it, the official compendium has errors.

I cannot update the official compendium, but I can update this downloader's data export.
In this version, over 450 entries has been corrected:

* 151 items missing power frequency. (e.g. Dantrag's Bracers, many energy weapons, dragon orbs, light sources etc.)
 * The downloader reports 152 fixes. One of them - Arrow of Fate - is duplicated.
* 79 empty glossaries, removed. (e.g. male, female, fang titan drake, etc.)
* 68 entries without "published" record. (e.g. Granny's Grief, Dreamheart, Deck of Many Things etc.)
* 60 entries moved to correct category:
 * 39 Superior implements moved from weapon to implement.
 * 19 Assassin's poisons moved from item to poison.
 * 2 Wondrous items moved to consumable.
* 35 entries that incorrectly says "basic melee attack". (e.g. Bane's Tactics, Feral Armor, Dancing Weapon, Kobold Piker etc.)
* 27 entries with formatting issues, such as
 * Content cut in the middle (e.g. Mirror of Deception),
 * "published" not properly formatted (e.g. Drow Poison),
 * Putting level and group role together (e.g. Trapped Chest), etc.
* 15 entries with inconsistent content:
 * 8 items that says a power is reproduced but isn't (e.g. Iron Wand, Shielding Wand etc.)
 * 4 armors that list all 6 types instead of "Any" (e.g. Winged Armor, Wall Armor etc.)
 * 2 feats with different level format (Powerful Lure, Traveler's Celerity)
 * 1 Xenda-Dran's Array should be heroic tier.
* 12 skills missing "Imporvising with (this skill)" subtitle. (Arcana, Bluff, Diplomacy, etc.)
* 9 races missing meta data such as abilities or size.
* 3 typos. ("bit points" of Cambion Stalwart, power keyword of Fifth Sword of Tyr, Primal Grove component)
* 3 entries with missing or wrong content (Kord's Relentlessness, Orium Implement, Rings of the Akarot)

If you find similar mistakes that aren't fixed, please [file an issue](https://github.com/Sheep-y/trpg-dnd-4e-db/issues/).

## Source code and building ##

* Viewer source code is in html folder.
* Downloader source code is in java folder, and use libraries in java_lib folder.
* Both use resources at the root (license) and/or in the resource folder.
* Use Ant (build.xml) to compile both downloader and viewer into an executable jar.
* The jar can also be extracted to a new folder; use Ant to move the extracted files back to original structure.

Part of the build process uses the [CocoDoc](https://github.com/Sheep-y/CocoDoc/) app builder, which is bundled and must run in GUI.
Try to use 64 bits java runtime; 32 bits may stackoverfow on js minify, but won't affect functionality.

If you use an IDE, be careful not to export data to project folder.
Otherwise, it can take a long time for the IDE to scan all the data files.

Note that this program use sqljet to access sqlite database, but the data cannot be read by other SQLite libraries.
I understand that the file format didn't change, so it should be a sqljet issue, which is discontinued.
Since I cannot find an equally light alternative and I haven't had other issues, I'm keeping it and hoping someone can find what's wrong someday.

<small>
Code, documentations, and related resources are open source and licensed under <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>