package db4e.data;

import java.util.Arrays;

/**
 * Represents a data entry
 */
public class Entry {
   private String id; // Compendium url of this entry
   private String name; // Display name
   private String[] fields; // Field data loaded from compendium. Not loaded until export.
   private String content; // Actual content. Not loaded until export.

   // Transformed data for export
   public String fulltext;   // Full text index text - without name and flavour
   public Object[] meta;    // Transform field data
   public String data;     // Processed data text

   public Entry() {}

   public EntryDownloaded downloaded() { throw new UnsupportedOperationException(); }

   public String getId() {
      return id;
   }

   public void setId( String id ) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName( String name ) {
      this.name = name;
   }

   public String[] getFields() {
      return fields;
   }

   public String getField( int i ) {
      return fields[ i ];
   }

   public void setFields( String[] fields ) {
      this.fields = fields;
   }

   public String getContent() {
      return content;
   }

   public void setContent( String content ) {
      this.content = content;
   }

   public String getUrl() {
      return getId();
   }

   public <T extends Entry> T cloneTo( T copy ) {
      copy.setId( getId() );
      copy.setName( getName() );
      copy.setFields( Arrays.copyOf( getFields(), getFields().length ) );
      copy.setContent( getContent() );
      return copy;
   }

   @Override public String toString() {
      return id + " " + name;
   }
}