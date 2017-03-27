package db4e.data;

public class EntryDownloaded extends Entry {

   private boolean contentDownloaded; // Indicate whether this entry has content in database.

   public EntryDownloaded ( String id, String name ) {
      setId( id );
      setName( name );
   }

   public EntryDownloaded ( String id, String name, Object[] fields ) {
      this( id, name );
      setFields( fields );
   }

   @Override public EntryDownloaded downloaded() {
      return this;
   }

   public boolean isContentDownloaded() {
      return contentDownloaded;
   }

   public void setContentDownloaded( boolean contentDownloaded ) {
      this.contentDownloaded = contentDownloaded;
   }

   public String getUrl() {
      return "http://www.wizards.com/dndinsider/compendium/" + getId();
   }

   @Override public <T extends Entry> T cloneTo( T copy ) {
      copy = super.cloneTo(copy);
      if ( copy instanceof EntryDownloaded )
         ( (EntryDownloaded) copy ).setContentDownloaded( isContentDownloaded() );
      return copy;
   }
}