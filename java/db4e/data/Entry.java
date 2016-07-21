package db4e.data;

/**
 * Represents a data entry
 */
public class Entry {
   public final String id; // Compendium url of this entry
   public final String name; // Display name
   public String[] fields; // Field data loaded from compendium. Not loaded until export.
   public boolean contentDownloaded; // Indicate whether this entry has content in database.
   public String content; // Actual content. Not loaded until export.

   // Transformed data for export
   public String display_name; // Converted name for export
   public String shortid;     // Simplified id for export. Set to "null" (String) to skip export.
   public String fulltext;   // Full text index text - without name and flavour
   public Object[] meta;    // Transform field data
   public String data;     // Processed data text

   public Entry ( String id, String name ) {
      this.id = id;
      this.name = name;
   }

   public Entry ( String id, String name, String[] fields ) {
      this( id, name );
      this.fields = fields;
   }

}