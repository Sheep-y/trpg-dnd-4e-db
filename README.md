# Offline 4e Database #

**For executable code please see <a href="http://github.com/Sheep-y/trpg-dnd-4e-db/tree/deployment">deployment branch</a>.**

This app can be used to reterives and locally stores entries from online 4e <a href="http://www.wizards.com/dndinsider/compendium/database.aspx">D&D Compendium</a>.
 <br/>
Stored entries can be exported, then browsed and searched using a Google-like index with multi-word term search, wildcast, exclusion, inclusion ("OR"), and even regular expression.

This is a fan-made project and does not contain any copyrighted data.

## How To Download Data ##

# You need an active <a href="http://ddi.wizards.com/">Dungeons & Dragons Insider subscription</a> to retrieve data.
# <a href='http://www.java.com/en/'>Download</a> and install Java (latest version 8 or above).
# <a href='https://github.com/Sheep-y/trpg-dnd-4e-db/releases/'>Download</a> the downloader exe (Windows) or downloader jar (Linux/Mac).
# Open a folder for it, put it in, and run it.
  # (Jar version) If the file is opened by a compression program, tell the program to not open .jars file and let Java handles it, or open console and run "java -jar 4e_downloader.jar".  The downloader is purely graphical.
# In the downloader, fill in DDI username and password, then click "Download".
  # Download can be stopped and resumed any time.
  # See in-downloader help for more details.
# Once all data is downloaded, you can export the data to an HTML file, which can be opened in any browser.

## Development ##

A built script is included to compile everything into executable jar.
The "make exe" build task can use <a href='http://launch4j.sourceforge.net/'>launch4j</a> to produce the exe file from the jar.

When importing the project into your favorite IDE, consider using the src folder as root so that the IDE won't scan data folder for changes.
Because, when fully loaded, there will be 25k+ files for it to scan.

<small>
Code and documentations are open source and licensed under <a href="www.gnu.org/licenses/agpl.html‎">GNU AGPL v3</a>. <br/>
D&D and Dungeons & Dragons are trademarks of Wizards of the Coast LLC.
</small>