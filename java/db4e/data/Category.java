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
   public final IntegerProperty total_entry = new SimpleIntegerProperty();
   public final IntegerProperty downloaded_entry = new SimpleIntegerProperty();

   public final String[] fields; // Name (id) of compendium fields
   public final List<Entry> entries = new ArrayList<>(); // Entry list

   // Transformed data for export
   public String[] meta; // Transform field list
   public Entry[] sorted; // Sorted entry list

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
}