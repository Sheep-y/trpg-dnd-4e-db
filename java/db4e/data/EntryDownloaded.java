package db4e.data;

public class EntryDownloaded extends Entry {

   private boolean hasContent; // Indicate whether this entry has content in database.

   public EntryDownloaded ( String id, String name ) {
      setId( id );
      setName( name );
   }

   public EntryDownloaded ( String id, String name, Object[] fields ) {
      this( id, name );
      setFields( fields );
   }

   public boolean hasContent() {
      return hasContent;
   }

   public void setHasContent( boolean hasContent ) {
      this.hasContent = hasContent;
   }

   public String getUrl() {
      return "http://www.wizards.com/dndinsider/compendium/" + getId();
   }

   @Override public <T extends Entry> T cloneTo( T copy ) {
      copy = super.cloneTo(copy);
      if ( copy instanceof EntryDownloaded )
         copy.setHasContent( hasContent() );
      return copy;
   }
}