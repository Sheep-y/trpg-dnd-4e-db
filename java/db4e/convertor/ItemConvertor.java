package db4e.convertor;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.logging.Level;

public class ItemConvertor extends FieldSortConvertor {

   public ItemConvertor ( Category category, boolean debug ) {
      super( category, 0, debug ); // Sort by category
   }

   @Override protected void correctEntry(Entry entry) {
      switch ( entry.shortid ) {
         case "item467": // Alchemical Failsafe
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ At-Will</h2>" );
            break;

         case "item1007": // Dantrag's Bracers
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Daily</h2>" );
            entry.data = entry.data.replaceFirst( "Power ✦ </h2>", "Power ✦ Encounter</h2>" );
            break;

         default:
            if ( debug ) {
               if ( entry.data.contains( "Power ✦ <" ) )
                  log.log( Level.WARNING, "Power without frequency: {0} {1}", new Object[]{ entry.shortid, entry.name } );
               else if ( entry.data.contains( " ✦ (Free Action)</h2>" ) )
                  log.log( Level.WARNING, "Power without at-will: {0} {1}", new Object[]{ entry.shortid, entry.name } );
            }
      }
   }
}
