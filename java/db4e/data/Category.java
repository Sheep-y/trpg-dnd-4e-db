package db4e.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a data category
 */
public class Category {
   public final String id; // Compendium id
   public final String name; // Display name
   // Number of entry on the compendium. Either 0 (no listing) or the final count (listing done), no middle ground.
   public final IntegerProperty total_entry = new SimpleIntegerProperty();
   // Number of entry with downloaded content. Will increase during the download process.
   public final IntegerProperty downloaded_entry = new SimpleIntegerProperty();

   public String[] fields; // Name (id) of compendium fields
   public final List<Entry> entries = new ArrayList<>(); // Entry list
   public Map<String, List<String>> index; // Lookup name to entry id

   public Category( String id, String name, String[] fields ) {
      this.id = id;
      this.name = name;
      this.fields = fields;
   }

   public String getName() { return name; }
   public IntegerProperty totalEntryProperty() { return total_entry; }
   public IntegerProperty downloadedEntryProperty() { return downloaded_entry; }

   public int getExportCount () {
      return entries.size();
   }
}