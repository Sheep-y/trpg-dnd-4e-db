package db4e.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data category
 */
public class Category {
   public final String id;
   public final List<String> columns = new ArrayList<>();
   public int size; // Listed entry count

   public Category(String id) {
      this.id = id;
   }

}
