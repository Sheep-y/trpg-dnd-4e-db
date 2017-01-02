package db4e.converter;

import db4e.data.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemeConverter extends Converter {

   public ThemeConverter ( Category category, boolean debug ) {
      super( category, debug );
   }

   @Override protected void initialise() {
      category.meta = new String[]{ "Prerequisite", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxPrerequisite = Pattern.compile( "<b>Prerequisite: </b>([^<(]+)" ).matcher( "" );

   @Override protected void convertEntry () {
      meta( "", entry.fields[0] );
      super.convertEntry();
      if ( regxPrerequisite.reset( entry.data ).find() ) {
         String prerequisite = regxPrerequisite.group( 1 );
         int pos = prerequisite.indexOf( "The games of court" );
         if ( pos > 0 ) prerequisite = prerequisite.substring( 0, pos-1 );
         meta( 0, prerequisite );
      }
   }
}