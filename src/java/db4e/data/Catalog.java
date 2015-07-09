package db4e.data;

import java.io.File;
import java.util.Map;

public class Catalog {
   private final String main_file = "4e_database.html";
   private final String data_folder = "4e_database_files";
   public Map<String, Category> categories;

   public void clear() {
      categories.clear();
   }

   public void load( File basepath ) {
      File f = new File(basepath, data_folder + "/catalog.js");
      if (!f.exists()) {
         return;
      }
   }

   public void addCategory( String name ) {
      if ( categories.containsKey( name ) )
         categories.put( name, new Category( name ) );
   }

}
