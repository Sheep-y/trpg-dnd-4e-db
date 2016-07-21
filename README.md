# 4e Compendium Downloader #

![Screenshot of program](https://raw.githubusercontent.com/Sheep-y/trpg-dnd-4e-db/master/res/img/History%20-%20v3.5%20viewer.png)

Version 3.5.0.1

This app can be used to reterives and locally stores entries from online 4e [D&D Compendium](http://www.wizards.com/dndinsider/compendium/database.aspx).
 <br/>
Stored entries can be exported, then browsed and searched with exact phase search, wildcast, exclusion, join ("OR"), and even regular expression.

This is a fan-made project and does not come with copyrighted data.

## How To Download Data ##

1. You need an active [Dungeons & Dragons Insider subscription](http://ddi.wizards.com/) to retrieve data.
2. [Download](http://www.java.com/) and install Java (latest version 8 or above).
3. [Download](https://github.com/Sheep-y/trpg-dnd-4e-db/releases/) the downloader exe (Windows) or downloader jar (Linux/Mac).
4. Open a folder for the downloader, put it in, and run it.
   5. Jar version: If double clicking the jar file does not work, open console and run "java -jar 4e_compendium_downloader.jar".
6. In the downloader, fill in DDI username and password, then click "Download".
   7. Download can be stopped and resumed any time.
   8. See in-downloader help for details and troubleshoots.
9. Once all data is downloaded, you can export the data to an HTML file, which can be opened in any browser.

If you find a typo or obvious mistake in the data, please [file an issue](https://github.com/Sheep-y/trpg-dnd-4e-db/issues/).
I cannot update the official compendium, but I can update this downloader's data export.

## Building ##

* Viewer source code is in html folder.
* Downloader source code is in java folder.
* Use Ant (build.xml) to compile both into an executable jar.
* The jar can also be extracted to a new folder; use Ant to move the extracted files back to original structure.

Part of the build process uses the [CocoDoc](https://github.com/Sheep-y/CocoDoc/) app builder, which is bundled and must run in GUI.
Try to use 64 bits java runtime; 32 bits may stackoverfow on js minify, but won't affect functionality.

If you use an IDE, be careful not to export data to project folder.
Otherwise, it can take a long time for the IDE to scan all the data files.

<small>
Code, documentations, and related resources are open source and licensed under <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>