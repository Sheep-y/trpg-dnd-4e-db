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
      { "data.btn.save", "Save" },
      { "data.dlg.location.title", "Select offline compendium to update" },
      { "data.dlg.location.filter.db", "Offline Compendium" },
      { "data.dlg.location.filter.any", "Any file" },

      { "web.title" , "Browser" },

      // Log messages
      { "log.title", "Log" },
      { "log.init" , "System initialised." },
      { "log.l10n" , "Setting language to {0}." },
      { "log.data.err.cannot_read", "Cannot read compendium: {0}." },
      { "log.data.err.not_compendium", "Selected file is not offline 4e compendium." },
      { "log.web.run" , "Running {0}." },
      { "log.web.error" , "Browser error: {0}." },
   };

   @Override protected Object[][] getContents() {
      return contents;
   }
}