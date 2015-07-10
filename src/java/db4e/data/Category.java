package db4e.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data category
 */
public class Category {
   public final String id;
   public int size; // Listed entry count
   public final List<Object> columns = new ArrayList<>();
   public final List<Entry> entries = new ArrayList<>();

   public Category(String id) {
      this.id = id;
   }

   @Override public String toString() {
      StringBuilder str = new StringBuilder().append( id ).append( ' ' );
      str.append( entries.size() ).append( '/' ).append( size ).append( " (" );
      if ( ! columns.isEmpty() ) {
         columns.forEach( col -> str.append( col ).append( ',' ) );
         str.setLength( str.length()-1 );
      }
      return str.append( ')' ).toString();
   }
}
