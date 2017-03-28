package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeityConverter extends Converter {

   public DeityConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise() {
      category.meta = new String[]{ "Domains", "Alignment", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxDomain  = Pattern.compile( "<b>Domain: </b>([^<]+)" ).matcher( "" );

   @Override protected void convertEntry () {
      meta( "", meta( 0 ), meta( 1 ) );
      super.convertEntry();
      if ( find( regxDomain ) )
         meta( 0, regxDomain.group( 1 ) );
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( 2 ).compareTo( b.getSimpleField( 2 ) );
      if ( diff != 0 ) return -diff;
      return super.sortEntity( a, b );
   }
}