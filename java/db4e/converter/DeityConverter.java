package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeityConverter extends Converter {

   protected int DOMAINS = 0;

   public DeityConverter ( Category category ) {
      super( category );
   }

   @Override protected void initialise() {
      category.fields = new String[]{ "Domains", "Alignment", "SourceBook" };
      super.initialise();
   }

   private final Matcher regxDomain = Pattern.compile( "<b>Domain: </b>([^<]+)" ).matcher( "" );

   @Override protected void convertEntry () {
      meta( "", meta( 0 ), meta( 1 ) );
      super.convertEntry();
      if ( find( regxDomain ) )
         meta( DOMAINS, regxDomain.group( 1 ) );
   }

   @Override protected int sortEntity ( Entry a, Entry b ) {
      int diff = a.getSimpleField( 2 ).compareTo( b.getSimpleField( 2 ) );
      return diff == 0 ? super.sortEntity( a, b ) : -diff;
   }

   @Override protected Set<String> getLookupName( Entry entry, Set<String> list ) {
      if ( entry.getName().startsWith( "The " ) )
         list.add( entry.getName().substring( 4 ) );
      if ( ! meta( DOMAINS ).isEmpty() )
         list.addAll( Arrays.asList( meta( DOMAINS ).split( ", " ) ) );
      return super.getLookupName( entry, list );
   }
}