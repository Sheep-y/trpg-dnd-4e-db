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
      { "data.status.unloading", "Unloading data." },
      { "data.status.no_folder", "Please select data folder." },
      { "data.status.reading", "Reading data." },
      { "data.status.ready", "Ready for update." },
      { "data.btn.location", "_Open Compendium" },
      { "data.btn.delete", "_Delete Data" },
      { "data.btn.delete_hint", "Delete local data." },
      { "data.btn.resave", "Re_save Data" },
      { "data.btn.resave_hint", "Reparse and reindex saved data to fix data corruption." },
      { "data.btn.update", "_Update Data" },
      { "data.btn.update_hint", "Fetch data from official compendium and save to disk." },
      { "data.dlg.delete.title", "Delete Data" },
      { "data.dlg.delete.text", "Delete saved compendium data?" },
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
      { "log.writer.delele.entry", "Deleting entry {0}" },
      { "log.writer.delele.category", "Deleting category {0}" },
      { "log.writer.delele.catalog", "Deleting catalog {0}" },
      { "log.writer.delele.failed", "Cannot delete {0}" },

      { "log.web.run" , "Running {0}." },
      { "log.web.error" , "Browser error: {0}." },
   };

   @Override protected Object[][] getContents() {
      return contents;
   }
}