package db4e.data;

public class EntryDownloaded extends Entry {

   private boolean hasContent; // Indicate whether this entry has content in database.

   public EntryDownloaded ( String id, String name ) {
      synchronized ( this ) {
         setId( id ).setName( name );
      }
   }

   public EntryDownloaded ( String id, String name, Object[] fields ) {
      this( id, name );
      synchronized ( this ) {
         setFields( fields );
      }
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
      synchronized ( copy ) {
         copy = super.cloneTo( copy );
         if ( copy instanceof EntryDownloaded )
            copy.setHasContent( hasContent() );
      }
      return copy;
   }
}