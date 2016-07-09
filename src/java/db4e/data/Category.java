package db4e.data;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a data category
 */
public class Category {
   public final StringProperty id = new SimpleStringProperty();
   public final StringProperty name = new SimpleStringProperty();
   public final IntegerProperty total_entry = new SimpleIntegerProperty();
   public final IntegerProperty downloaded_entry = new SimpleIntegerProperty();
   public final IntegerProperty exported_entry = new SimpleIntegerProperty();

   public final String[] meta;
   public final List<Entry> entries = new ArrayList<>();

   public Category( String[] fields ) {
      this.meta = fields;
   }

   @Override public String toString () {
      StringBuilder str = new StringBuilder().append( id ).append( ' ' );
      str.append( entries.size() ).append( '/' ).append( total_entry );
      return str.toString();
   }
}