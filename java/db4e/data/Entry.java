package db4e.data;

import java.util.Arrays;

/**
 * Represents a data entry
 */
public class Entry {
   private String id; // Compendium url of this entry
   private String name; // Display name
   private Object[] fields; // Field data loaded from compendium. Not loaded until export.
   private String content; // Actual content. Not loaded until export.

   public Entry() {}

   public EntryDownloaded downloaded () { throw new UnsupportedOperationException(); }

   public String getId () {
      return id;
   }

   public void setId( String id ) {
      this.id = id;
   }

   public String getName () {
      return name;
   }

   public void setName( String name ) {
      this.name = name;
   }

   public int getFieldCount () {
      return fields.length;
   }

   public Object[] getFields () {
      return fields;
   }

   public String[] getSimpleFields () {
      int len = fields.length;
      try {
         return Arrays.copyOf( fields, len, String[].class );
      } catch ( ArrayStoreException ex ) {
         String[] result = new String[ len ];
         for ( int i = 0 ; i < len ; i++ )
            result[ i ] = getSimpleField( i );
         return result;
      }
   }

   public Object getField ( int i ) {
      return fields[ i ];
   }

   public String getSimpleField ( int i ) {
      Object result = fields[ i ];
      if ( result instanceof Object[] )
         result = ( (Object[]) result )[0];
      return result.toString();
   }

   public void setFields ( Object ... fields ) {
      this.fields = fields;
   }

   public void setField ( int i, Object field ) {
      this.fields[ i ] = field;
   }

   public String getContent () {
      return content;
   }

   public void setContent ( String content ) {
      this.content = content;
   }

   public String getUrl () {
      return getId();
   }

   public <T extends Entry> T cloneTo ( T copy ) {
      copy.setId( getId() );
      copy.setName( getName() );
      copy.setFields( Arrays.copyOf( getFields(), getFieldCount() ) );
      copy.setContent( getContent() );
      return copy;
   }

   @Override public String toString () {
      return id + " " + name;
   }
}