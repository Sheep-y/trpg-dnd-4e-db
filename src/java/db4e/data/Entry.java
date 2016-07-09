package db4e.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a data entry
 */
public class Entry {
   public final Category category;
   public final Map<String,Object> meta;
   public volatile String content;

   public Entry ( Category category, String id ) {
      assert( category != null && id != null );
      this.category = category;
      meta = new HashMap<>( category.meta.length, 1.0f );
      setMeta( "ID", id );
   }

   public String getId () { return getMeta( "ID" ).toString(); }
   public String getFileId () { return getId().replace( ".aspx?id=", "" ); }

   public Object getMeta ( String name ) {
      synchronized ( meta ) {
         return meta.get( name );
      }
   }

   public void setMeta ( String name, Object data ) {
      synchronized ( meta ) {
         meta.put( name, data );
      }
   }

}