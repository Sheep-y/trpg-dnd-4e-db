package db4e.data;

import javafx.collections.ObservableList;
import sheepy.util.ui.ObservableArrayList;

public class Catalog {
   public final ObservableList<Category> categories = new ObservableArrayList<>();

   public synchronized void clear() {
      categories.clear();
   }

   @Override public String toString () {
      if ( categories.size() <= 0 ) return "{}";
      StringBuilder str = new StringBuilder().append( '{' );
      categories.forEach( cat ->
         str.append( cat.id ).append( ':' ).append( cat.entries.size() ).append('/').append( cat.total_entry ) );
      str.setLength( str.length()-1 );
      return str.append( '}' ).toString();
   }
}