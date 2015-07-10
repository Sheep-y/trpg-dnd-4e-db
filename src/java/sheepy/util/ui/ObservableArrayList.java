package sheepy.util.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.collections.ModifiableObservableListBase;

/**
 * An ObservableList backed by an ArrayList.
 * @param <T> Element type
 */
public class ObservableArrayList<T> extends ModifiableObservableListBase<T> {

   private final List<T> list;

   public ObservableArrayList() {
      list = new ArrayList<>();
   }

   public ObservableArrayList( T ... items ) {
      this( Arrays.asList( items ) );
   }

   public ObservableArrayList( Collection<T> items ) {
      list = new ArrayList<>( items );
   }

   @Override public T get( int index ) { return list.get( index ); }
   @Override public int size() { return list.size(); }
   @Override protected void doAdd(int index, T element) { list.add( index, element ); }
   @Override protected T doSet(int index, T element) { return list.set( index, element ); }
   @Override protected T doRemove(int index) { return list.remove( index ); }
}
