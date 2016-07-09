package db4e.data;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a data category
 */
public class Category {
   public final String id;
   public final String name;
   public final IntegerProperty total_entry = new SimpleIntegerProperty();
   public final IntegerProperty downloaded_entry = new SimpleIntegerProperty();
   public final IntegerProperty exported_entry = new SimpleIntegerProperty();

   public final String type;
   public final String[] fields;
   public final List<Entry> entries = new ArrayList<>();

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
   public IntegerProperty exportedEntryProperty() { return exported_entry; }
}