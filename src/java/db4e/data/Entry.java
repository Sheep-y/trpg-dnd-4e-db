package db4e.data;

/**
 * Represents a data entry
 */
public class Entry {
   public final Category category;
   public final String id; // Last url part
   public final Object[] columns;
   public volatile String content;

   public Entry( Category category, String id, int col_count ) {
      this.category = category;
      this.id = id;
      columns = new Object[ col_count ];
   }

}
