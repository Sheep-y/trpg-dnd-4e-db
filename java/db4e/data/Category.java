package db4e.data;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a data category
 */
public class Category {
   public final String id; // Compendium id
   public final String name; // Display name
   public final String type; // Manually assigned category group - PC and DM.
   // Number of entry on the compendium. Either 0 (no listing) or the final count (listing done).
   public final IntegerProperty total_entry = new SimpleIntegerProperty();
   // Number of entry with downloaded content. Will increase during the download process.
   public final IntegerProperty downloaded_entry = new SimpleIntegerProperty();
   // Negative if some in-database entries are blacklisted and won't be exported.
   // Positive if some non-database entries are added into this category.
   // Must be set before exporting.
   public final IntegerProperty exported_entry_deviation = new SimpleIntegerProperty();

   public final String[] fields; // Name (id) of compendium fields
   public final List<Entry> entries = new ArrayList<>(); // Entry list

   public String[] meta; // Transform field list for export.
   public Entry[] sorted; // Sorted entry list for export.

   public Category( String id, String name, String type, String[] fields ) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.fields = fields;
   }

   @Override public String toString () {
      StringBuilder str = new StringBuilder().append( id ).append( ' ' );
      str.append( entries.size() ).append( '/' ).append( total_entry );
      return str.toString();
   }

   public String getName() { return name; }
   public IntegerProperty totalEntryProperty() { return total_entry; }
   public IntegerProperty downloadedEntryProperty() { return downloaded_entry; }

   public int getExportCount () {
      return total_entry.get() + exported_entry_deviation.get();
   }
}