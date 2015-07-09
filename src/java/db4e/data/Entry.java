package db4e.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data entry
 */
public class Entry {
   public final Category category;
   public final String id; // Last url part
   public final List<String> columns;
   public volatile String content;

   public Entry(Category category, String id, int count ) {
      this.category = category;
      this.id = id;
      columns = new ArrayList<String>(count);
   }

}
