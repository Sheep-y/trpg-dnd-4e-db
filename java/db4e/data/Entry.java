package db4e.data;

/**
 * Represents a data entry
 */
public class Entry {
   private String id; // Compendium url of this entry
   private String name; // Display name
   private String[] fields; // Field data loaded from compendium. Not loaded until export.
   private String content; // Actual content. Not loaded until export.
   public boolean contentDownloaded; // Indicate whether this entry has content in database.

   // Transformed data for export
   public String display_name; // Converted name for export
   public String shortid;     // Simplified id for export
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

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String[] getFields() {
      return fields;
   }

   public String getField( int i ) {
      return fields[ i ];
   }

   public void setFields(String[] fields) {
      this.fields = fields;
   }

   public String getContent() {
      return content;
   }

   public void setContent(String content) {
      this.content = content;
   }

   /**
    * Return an unconverted copy of this object
    */
   public Entry clone() {
      assert( contentDownloaded );
      Entry copy = new Entry( id, name );
      copy.fields = this.fields;
      copy.contentDownloaded = true;
      copy.content = this.content;
      return copy;
   }

   public String getUrl() {
      return "http://www.wizards.com/dndinsider/compendium/" + id;
   }

}