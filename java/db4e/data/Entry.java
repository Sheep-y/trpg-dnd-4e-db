package db4e.data;

import java.util.Arrays;

/**
 * Represents a data entry.
 * Access are NOT synchronised.  Read and write MUST be synchronised to the entry object.
 */
public class Entry {
   private String id; // Compendium url of this entry
   private String name; // Display name
   private Object[] fields; // Field data loaded from compendium. Not loaded until export.
   private String content; // Actual content. Not loaded until export.

   public Entry() {}

   public final String getId () {
      return id;
   }

   public final Entry setId( String id ) {
      this.id = id;
      return this;
   }

   public final String getName () {
      return name;
   }

   public final Entry setName( String name ) {
      this.name = name;
      return this;
   }

   public final int getFieldCount () {
      return fields.length;
   }

   public final Object[] getFields () {
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

   public final Object getField ( int i ) {
      return fields[ i ];
   }

   public String getSimpleField ( int i ) {
      Object result = fields[ i ];
      if ( result instanceof Object[] )
         result = ( (Object[]) result )[0];
      return result.toString();
   }

   public final Entry setFields ( Object ... fields ) {
      this.fields = fields;
      return this;
   }

   public final Entry setField ( int i, Object field ) {
      this.fields[ i ] = field;
      return this;
   }

   public final String getContent () {
      return content;
   }

   public final Entry setContent ( String content ) {
      this.content = content;
      return this;
   }

   public String getUrl () {
      return getId();
   }

   // Return false if entry listing is in database, but content has not yet been downloaded.
   public boolean hasContent() {
      return true;
   }

   public Entry setHasContent( boolean hasContent ) {
      throw new UnsupportedOperationException();
   }

   public <T extends Entry> T cloneTo ( T copy ) {
      copy.setId( getId() );
      copy.setName( getName() );
      copy.setFields( Arrays.copyOf( getFields(), getFieldCount() ) );
      copy.setContent( getContent() );
      return copy;
   }

   @Override public final String toString () {
      return id + " " + name;
   }
}