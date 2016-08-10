# 4e Compendium Downloader #

## Version 3.5.2 development branch ##

![Screenshot of program](https://raw.githubusercontent.com/Sheep-y/trpg-dnd-4e-db/master/res/img/History%20-%20v3.5%20viewer.png)

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
This downloader fixes over 480 entries during export:

* 151 items missing power frequency. (Dantrag's Bracers, many energy weapons, dragon orbs, light sources etc.)
 * The downloader logs 152 fixes. One of them - Arrow of Fate - is a duplicate.
* 79 empty glossaries, removed. (male, female, fang titan drake, etc.)
* 68 entries without "published" record. (Granny's Grief, Dreamheart, Deck of Many Things etc.)
* 60 entries moved to correct category. (Superior implements, Assassin's poisons, and Consumable wondrous)
* 35 entries that wrongly says "basic melee attack". (Bane's Tactics, Feral Armor, Dancing Weapon, Kobold Piker etc.)
* 21 entries with other inconsistent content. (Winged Armor type, Xenda-Dran Array tier, etc.)
* 20 entries with formatting issues. (Mirror of Deception, etc.)
* 15 entries with missing content. (Kord's Relentlessness, Orium Implement, Rings of the Akarot etc.)
* 3 typos. ("bit points" of Cambion Stalwart, power keyword of Fifth Sword of Tyr, Primal Grove component)
* 48 entries with missing/wrong listing data not caused by above issues. (Wild Elf, Enlarge Familiar, Trapped Chest etc.)
* In addition, over a hundred artifacts and item sets are manually classified.

If you find similar mistakes that aren't fixed, please [file an issue](https://github.com/Sheep-y/trpg-dnd-4e-db/issues/).

## Source code and building ##

* Viewer source code is in html folder.
* Downloader source code is in java folder, and use libraries in java_lib folder.
* Both use resources at the root (license) and/or in the resource folder.
* Use Ant (build.xml) to compile viewer and then downloader into an executable jar.
* The jar can also be extracted to a new folder; use Ant to move the extracted files back to original structure.

The viewer is built with [CocoDoc](https://github.com/Sheep-y/CocoDoc/) app builder, which is bundled and must run in GUI.
Try to use 64 bits java runtime; 32 bits may stackoverfow on js minify, but won't affect functionality.

The downloader use sqljet to access sqlite database, but the data cannot be read by other SQLite libraries.
It might be a sqljet issue, which is discontinued.
Since it is very light and has no other issues, I'm keeping it and hoping someone can find what's wrong someday.

<small>
Code, documentations, and related resources are open source and licensed under <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>