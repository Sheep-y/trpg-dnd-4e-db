# 4e Compendium Downloader #

## Version 3.6.2 development branch ##

[![3 min. intro video](https://raw.githubusercontent.com/Sheep-y/trpg-dnd-4e-db/master/res/img/Frontpage.jpg)](https://youtu.be/aNDze9Ok5fE)

This program can fetches, stores, and exports D&D 4th edition resources from [D&D Compendium](http://www.wizards.com/dndinsider/compendium/database.aspx). <br/>
Exported entries can be browsed and searched offline with exact phase search, wildcast, exclusion, join ("OR"), and more.

本應用能從四代龍與地下城的[官方資料庫](http://www.wizards.com/dndinsider/compendium/database.aspx)提取、儲存、及匯出資料，以便進行離線的威力搜尋。<br/>
下載器的說明只限英語。匯出資料後的搜尋介面可以在說明頁中切換成中文。

This is a fan project and does not come with copyrighted data.

## How To Download Data ##

[Video guide](https://youtu.be/aNDze9Ok5fE) available.

1. You need an active [Dungeons & Dragons Insider subscription](http://ddi.wizards.com/) to fetch data.
   1. A new subscription can be purchased from [digitalriver](http://gc.digitalriver.com/store/dndi/html/pbPage.wizards).
2. [Download](http://www.java.com/) and install Java (version 8 or above).
3. [Download](https://github.com/Sheep-y/trpg-dnd-4e-db/releases/) the downloader exe (Windows) or downloader jar (Linux/Mac).
4. Create a folder for the downloader, put it in, and run it.
   1. Jar version: If double clicking the jar file does not work, open console/terminal and run "java -jar 4e_compendium_downloader.jar". This should launch the program.
5. In the downloader, fill in DDI username and password, then click "Download".
   1. Download can be stopped and resumed any time.
   2. See [in-program help](http://htmlpreview.github.io/?https://github.com/Sheep-y/trpg-dnd-4e-db/blob/master/res/downloader_about.html) for details and troubleshoots.
6. Once all data is downloaded, you can export the data to an HTML file, which can be opened in browsers.
   1. The HTML file works totally offline, and can be copied to your preferred mobile devices.

### Compatibility ###

The downloader is tested with Sun Java 8 and 9.
If you use [OpenJDK](http://openjdk.java.net/), you also need [OpenJFX](http://openjdk.java.net/projects/openjfx/). <br/>
The viewer is tested on Chrome 63, Edge 15, Firefox 57, IE 11, Android Chrome 63, Android Firefox 57, and Android UC Browser 11.5. <br/>
Mac and iOS cannot be supported.  Walled garden is walled.

## Differences from Official Compendium ##

This downloader will create new data columns for easier data filter:

* Race: Origin (and keywords).
* Background: Benefit (Replace Associated Skills).
* Theme: Prerequisites.
* Power: Type (Frequency + Type) and Keywords (Range + Keywords).
* Feat: Prerequisite.
* Item: Split into 4 categories (Item, Weapon, Implement, and Armor). Add Type column.
* Companion: Size and Type.
* Monster: Size and Type.
* Terrain: Group and Level.  Combine with Trap to control category count.

Many existing columns are also enhanced or corrected, such as multi-source classes.

### Fixed errors ###

Let's face it, the official compendium has errors.
This downloader fixes over 2040 entries during export:

* 551 powers classified as "Theme Power" or "Multiclass" (actually Spellscarred), corrected.
* 299 entries with inconsistency in wordings. (Winged Armor type, Xenda-Dran Array tier, Racial power without type etc.)
* 233 entries with bold and font size issues. (Deluge, Dark Toxin etc.)
* 191 entries moved to correct category. (Superior implements, Lair items, Assassin's poisons, Consumable wondrous etc.)
* 173 entries with wrong listing data not caused by other issues. (Multi-source classes, traps mixing up role and type etc.)
* 173 entries with line breaks, indent, and other formatting issues. (Silver Hands of Power, Spider Cactus etc.)
* 151 entries missing power frequency. (Bending Branch, Dantrag's Bracers, energy weapons, dragon orbs, lights etc.)
* 96 empty entries or non-resources, removed. (male, female, fang titan drake, Fastpaw background, etc.)
* 94 entries with missing listing data not caused by other issues. (Subrace abilities, Heroic feat tier etc.)
* 68 entries without "published" record. (Granny's Grief, Dreamheart, Deck of Many Things etc.)
* 40 entries with missing rule keywords. (Quick Kill range, Cloudburst spirit keyword, Blistered Soul type etc.)
* 35 entries that wrongly says "basic melee attack". (Bane's Tactics, Feral Armor, Dancing Weapon, Kobold Piker etc.)
* 30 entries with wrong title colour (Shield of Prator, Shadowstep, Imprison etc.)
* 20 entries with other missing/wrong content. (Kord's Relentlessness, Rings of the Akarot, Hybrid Vampire Surge etc.)
* 17 typos. (Cambion Stalwart "bit points", Primal Grove "grp", Rubble Topple "Singe", Essential hybrid sourcebook etc.)
* 7 new glossary entries (Ability Scores, Artifact, Bloodlines, Implements, Item Set, Totem, and Wand.)
* In addition, over a hundred artifacts and item sets are manually classified.
(Some entries has multiple types of fix applied, e.g. Mirror of Deception has power frequency, formatting, and category fixed)

If you find similar mistakes that are not fixed, please [file an issue](https://github.com/Sheep-y/trpg-dnd-4e-db/issues/).

## Developer's Guide ##

This program has two parts: a downloader that fetch and export data, and a viewer that browse exported data.
The downloader will embed the viewer on build, so that there is only one deployable file.

* The [development branch](https://github.com/Sheep-y/trpg-dnd-4e-db/tree/development) is less vigorously tested but usually have more features and/or fixes.
* Downloader source code is in java folder, and use libraries in java_lib folder.
* Viewer source code is in html folder.  It is built with [CocoDoc](https://github.com/Sheep-y/CocoDoc/), also in java_lib.
* Both use resources at the root (license) and in the resource folder.
* Use Ant (build.xml) to 'make' an executable jar.  The make_exe target depends on [Launch4j](http://launch4j.sourceforge.net/).
* The jar can also be extracted to a new project folder; use Ant to move the extracted files back to original structure.

[SQLJet](https://sqljet.com/) is used to access sqlite database, but the data cannot be read by other SQLite libraries.
It may be a sqljet issue.
Since it is very light and has no other issues, I'm keeping it and hoping someone can find what's wrong someday.

<small>
Code, documentations, and related resources are open source and licensed under <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>