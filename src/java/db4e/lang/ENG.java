package db4e.lang;

import java.util.ListResourceBundle;

public class ENG extends ListResourceBundle {

   @Override public String getBaseBundleName() {
      return "db4e";
   }


   private static final Object[][] contents = new Object[][]{
      { "title", "Offline 4e compendium updater" },

      { "guide.title", "Guide" },
      { "guide.html" , "<h1>Work In Progress</h1>" },

      { "data.title", "Data" },
      { "data.btn.location", "Select compendium" },
      { "data.dlg.location.title", "Select offline compendium to update" },
      { "data.dlg.location.filter.db", "Offline Compendium" },
      { "data.dlg.location.filter.any", "Any file" },

      { "web.title" , "Browser" },

      // Log messages
      { "log.title", "Log" },
      { "log.init" , "System initialised." },
      { "log.l10n" , "Setting language to {0}." },
      { "log.malform", "File {0} is malformed: {1}" },
      { "log.cannot_read", "Cannot read file: {0}." },

      { "log.data.err.not_compendium", "Selected file is not offline 4e compendium." },

      { "log.updater.rebase", "Rebasing to {0}" },
      { "log.updater.stopped", "All changes written." },
      { "log.updater.stopping", "Waiting for changes to be written." },
      { "log.reader.done", "Local data read" },
      { "log.reader.entry", "Loaded {1} local entries for {0}" },
      { "log.reader.reading", "Reading local data {0}" },
      { "log.reader.stopped", "Stopped reading local data" },

      { "log.web.run" , "Running {0}." },
      { "log.web.error" , "Browser error: {0}." },
   };

   @Override protected Object[][] getContents() {
      return contents;
   }
}