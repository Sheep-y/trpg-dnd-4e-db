package db4e.data;

public class EntryDownloaded extends Entry {

   private boolean hasContent; // Indicate whether this entry has content in database.

   public EntryDownloaded ( String id, String name ) {
      setId( id ).setName( name );
   }

   public EntryDownloaded ( String id, String name, Object[] fields ) {
      this( id, name );
      setFields( fields );
   }

   @Override public boolean hasContent() { return hasContent;
   }

   @Override public EntryDownloaded setHasContent( boolean hasContent ) {
      this.hasContent = hasContent;
      return this;
   }

   @Override public String getUrl() {
      return "http://www.wizards.com/dndinsider/compendium/" + getId();
   }

   @Override public <T extends Entry> T cloneTo( T copy ) {
      copy = super.cloneTo( copy );
      if ( copy instanceof EntryDownloaded )
         copy.setHasContent( hasContent() );
      return copy;
   }
}