# Offline 4e Database #

This app can be used to reterives and locally stores entries from online 4e [D&D Compendium](http://www.wizards.com/dndinsider/compendium/database.aspx).
 <br/>
Stored entries can be exported, then browsed and searched using a Google-like index with multi-word term search, wildcast, exclusion, inclusion ("OR"), and even regular expression.

This is a fan-made project and does not contain any copyrighted data.

## How To Download Data ##

# You need an active [Dungeons & Dragons Insider subscription](http://ddi.wizards.com/) to retrieve data.
# [Download](http://www.java.com/) and install Java (latest version 8 or above).
# [Download](https://github.com/Sheep-y/trpg-dnd-4e-db/releases/) the downloader exe (Windows) or downloader jar (Linux/Mac).
# Open a folder for it, put it in, and run it.
  # (Jar version) If the file is opened by a compression program, tell the program to not open .jars file and let Java handles it, or open console and run "java -jar 4e_downloader.jar".  The downloader is purely graphical.
# In the downloader, fill in DDI username and password, then click "Download".
  # Download can be stopped and resumed any time.
  # See in-downloader help for more details.
# Once all data is downloaded, you can export the data to an HTML file, which can be opened in any browser.

## Development ##

A built script is included to compile everything into an executable jar.
Part of the build process uses the [CocoDoc](https://github.com/Sheep-y/CocoDoc/) app builder, which is bundled and must run in GUI.

If you use an IDE, becareful not to export data to project folder.
Otherwise, it can take a long time for the IDE to scan all the data files.

<small>
Code, documentations, and related resources are open source and licensed under <a href="www.gnu.org/licenses/agpl.html‎">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>