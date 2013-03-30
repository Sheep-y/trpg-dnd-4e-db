<script type='text/javascript'>'use strict';
/*
 * data_model.js
 * Create data model in memory
 */


oddi.data = {
   category : { },

   find_in_list : function data_find_in_list( list, id ) {
      for ( var i = 0, len = list.length ; i < len ; i++ )
         if ( list[i][0] === id )
            return i;
      return -1;
   },

   /** Clear all loaded data or a category of data */
   clear : function data_clear( category ) {
      if ( category ) {
         if ( this.category[category] ) {
            delete this.category[category];
         } else {
            if ( console ) console.log("Warning: Cannot reset category "+category);
         }
      } else {
         this.category = {};
      }
   },

   /** Make sure a category exists and return it */
   create_category : function data_creaete_category( category, columns ) {
      var result = this.category[category];
      if ( this.category[category] ) return result;
      result = this.category[category] = new oddi.data.Category( category );
      result.columns = columns;
      result.dirty.index = true;
      return result;
   },

   /**
    * Load listing and index of all categories
    */
   load_all_index : function data_load_all_index( onload ) {
      var data = oddi.data.category;
      var loadLatch = new _.Latch( 1, onload );
      for ( var cat in data ) {
         loadLatch.count_up();
         data[cat].load_listing( function( cat ){
            cat.load_index( function() { loadLatch.count_down(); });
         })
      }
      loadLatch.count_down();
   },

   /**
    * Write changed data to file
    */
   write : function data_write( onload ) {
      oddi.writer.write_index( function data_write_index_done() {
         var data = oddi.data.category;
         var latch = new _.Latch( 1, onload );
         for ( var cat in data ) data[cat].write( function(){ latch.count_down(); } );
         latch.count_down();
      })
   },

   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    */
   preprocess : function data_preprocess( data ) {
      // Normalise input
      data = data.trim().replace( /\r\n?/g, '\n' );
      // Extract body
      data = data.match( /<body[^>]*>\s*((?:.|\n)*?)\s*<\/body\s*>/ );
      if ( !data ) return null;
      // Normalise whitespace
      data = data[1].replace( /[\n\s]+/g, ' ' );
      // Remove script and form tags
      data = data.trim().replace( /<script\b.*?<\/script\s*>/g, '' ).replace( /<input[^>]*>/g, '').replace( /<form[^>]*>|<\/form\s*>/g, '' );
      // Remove empty contents
      data = data.replace( /<div[^>]*>\s*<\/div\s*>/g, '' );
      // Compression - Removes quotes, links, nbsp etc.
      data = data.replace( /(<\w+ \w+=)"([^" ]+)"/g, '$1$2' );
      data = data.replace( /<br\s*\/>/g, '<br>' );
      data = data.replace( /&nbsp;/g, '\u00A0' );
      data = data.replace( /<\/?a(\s[^>]+)?>/g, '' );
      // TODO: convert ' and links
      return data.trim();
   },
}

oddi.data.Category = function( name, count ) {
   this.name = name;
   this.title = this.name.replace( /([A-Z])/g, ' $1' ).trim();
   this.clear(); // Re-create arrays to contain data.
   if ( count ) this.listing.length = count;
}
oddi.data.Category.prototype = {
   name: "",
   title: "",
   columns: [],
   listing: [],
   index: [],
   data: [],
   dirty: [],

   count : function data_cat_count(){
      return this.listing.length;
   },

   /** Load listing, if it has not been loaded */
   load_listing : function data_cat_load_listing( onload ){
      if ( ! this.listing.loaded ) this.reload_listing( onload );
      else onload( this );
   },

   /** Force reload listing, if it has not been loaded */
   reload_listing : function data_cat_reload_listing( onload ){
      var cat = this;
      oddi.reader.read_data_listing( this.name, function() {
         cat.listing.loaded = true;
         cat.dirty.listing = false;
         if ( onload ) onload( cat );
      });
   },

   /** Load index, if it has not been loaded */
   load_index : function data_cat_load_index( onload ){
      if ( ! this.index.loaded ) this.reload_index( onload );
      else onload( this );
   },

   reload_index : function data_cat_reload_index( onload ){
      var cat = this;
      oddi.reader.read_data_index( this.name, function() {
         cat.index.loaded = true;
         cat.dirty.index = false;
         if ( onload ) onload( cat );
      });
   },

   find : function data_cat_find( id ){
      return oddi.data.find_in_list( this.listing, id );
   },

   clear : function data_cat_clear() {
      this.columns = [];
      this.listing = [];
      this.listing.loaded = false;
      this.index = [];
      this.index.loaded = false;
      this.data = [];
      this.dirty = [];
      this.dirty.listing = true;
      this.dirty.index = true;
   },

   /** Insert or update an entry */
   update : function data_cat_update( id, columns, listing, content, onload ) {
      if ( content ) {
         var data = oddi.data;
         var i;
         // Existing category, check columns
         if ( this.columns.toString().toUpperCase() !== columns.toString().toUpperCase() ) {
            this.clear();
            this.columns = columns;
         }
         var i = this.find( id );
         if ( i < 0 ) i = this.listing.length;
         var cat = this;
         function data_cat_update_onload(){
            i = Math.abs( i );
            if ( cat.index[i] ) cat.remove_index( i );
            cat.listing[i] = listing;
            cat.data[i] = content;
            cat.dirty[i] = true;
            cat.dirty.listing = true;
            cat.update_index( i );
            if ( onload ) onload();
         }
         this.load_data( i, data_cat_update_onload );
      } else {
         _.warn( "No data or cannot parse data for "+this.name+"."+id ); //  TODO: i18n
      }
   },

   data_block : function data_url( index ) {
      var startIndex = Math.floor( index / 100 ) * 100;
      return startIndex;
   },

   load_data : function data_cat_load_data( index, onload ) {
      var start = this.data_block( index );
      // Requesting to load a non-existing new block, or an already loaded block, do nothing.
      if ( ( index === this.listing.length && start === index ) || this.dirty[start] !== undefined ) {
         if ( onload ) onload( cat );
         return;
      }
      var cat = this;
      oddi.reader.read_data( this.name, start, function( c, from, to ) {
         for ( var i = from ; i <= to ; i++ ) cat.dirty[i] = false;
         if ( onload ) onload( cat );
      });
   },

   /** Remove the index of an existing entry. For internal use. */
   remove_index : function data_cat_remove_index( cat, index ) {
      // Currently unused
   },

   /** Update the index of an existing entry. For internal use. */
   update_index : function data_cat_update_index( index ) {
      this.index[index] = this.data[index].replace( /<[^>]+>/g, ' ' ).replace( /\s+/g, ' ' ).trim();
      this.dirty.index = true;
   },

   /** Write changed data back to file */
   write : function data_cat_write() {
      if ( this.dirty.index ) {
         var cat = this;
         oddi.writer.write_data_index( this, function data_cat_write_index_done(){
            cat.dirty.index = false;
            if ( cat.dirty.listing ) oddi.writer.write_data_listing( cat, function data_cat_write_listing_done(){ cat.dirty.listing = false } );
            for ( var i = 0 ; i < cat.data.length ; i++ ) {
               if ( cat.dirty[i] ) {
                  var from = cat.data_block( i );
                  var to = Math.min( from+100, cat.data.length );
                  oddi.writer.write_data( cat, from, to );
                  for ( var j = from ; j < to ; j++ ) cat.dirty[j] = false;
               }
            }
         } );
      }
   },
}

</script>