package db4e.data;

import java.util.Map;

/**
 * Represents a data entry
 */
public class Entry {
   public final String id;
   public final String name;
   public String[] fields;
   public String content;

   public Map<String,Object> meta;

   public Entry ( String id, String name ) {
      this.id = id;
      this.name = name;
   }
   public Entry ( String id, String name, String[] fields ) {
      this( id, name );
      this.fields = fields;
   }

}