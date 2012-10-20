<script type='text/javascript'>'use strict';
/*
 * data_reader.js
 * Read data from persistent storage
 */

oddi.reader = {
   _loaded: {},
   _timeout: 20 * 1000,

   /**
     * Call _.js if onload is empty, or onread if it is not empty.
     */
   _clear : function cleanup( src, onerror ) {
      clearTimeout( this._loaded[src] );
      delete this._loaded[src];
      if ( onerror ) {
         if ( typeof( onerror ) == 'function' ) onerror( src );
         else _.warn( onerror );
      }
   },

   /**
      * Add jsonp call and timeout detection
      */
   _read: function reader_read( src, onload, onerror ) {
      function clear_timeout(){ oddi.reader._clear( src, onerror ); }
      this._loaded[src] = setTimeout( clear_timeout, this._timeout );
      _.js(src, function(a){
         // Onload. Normally callback should clear the timer, if not then we have error.
         if ( oddi.reader._loaded[src] ) clear_timeout();
         if ( onload ) onload();
      });
   },

   url : {
      index : function index() { return oddi.config.data_url+'/index.jsonp' },
      category_listing : function cat_listing( category ) { return oddi.config.data_url+'/'+category+'/listing.jsonp' },
      category_index : function cat_listing( category ) { return oddi.config.data_url+'/'+category+'/index.jsonp' },
      data : function data_url( category, from ) {
         return oddi.config.data_url+'/'+category+'/data'+from+'.jsonp';
      },
   },

   read_index: function reader_read_index( onload ) {
      this._read( this.url.index(), onload, 'Cannot read data' );
   },

   jsonp_index: function reader_jsonp_index( version, data ) {
      for ( var cat in data) oddi.data.create_category( cat, data[cat] );
      this._clear( this.url.index() );
   },

   read_data_listing: function reader_read_data_listing( category, onload ) {
      this._read( this.url.category_listing( category ), onload, 'Cannot read listing of '+category );
   },

   jsonp_data_listing: function reader_jsonp_data_listing( version, category, columns, data ) {
      var cat = oddi.data.category[category];
      cat.columns = columns;
      cat.listing = data;
      this._clear( this.url.category_listing( category ) );
   },

   read_data_index: function reader_read_data_index( category, onload ) {
      this._read( this.url.category_index( category ), onload, 'Cannot read index of '+category );
   },

   jsonp_data_index: function reader_jsonp_data_index( version, category, data ) {
      var cat = oddi.data.category[category];
      cat.index = data;
      this._clear( this.url.category_index( category ) );
   },

   read_data: function reader_read_data( category, from, onload ) {
      this._read( this.url.data( category, from ), onload, 'Cannot read data #' + from + ' of ' + category );
      return this.url.data_block( index );
   },

   jsonp_data: function reader_jsonp_data( version, category, startIndex, data ) {
      var ary = oddi.data.category[category].data;
      for ( var i = 0 ; i < data.length ; i++ ) {
         ary[startIndex+i] = data[i];
      }
      this._clear( this.url.data( category, startIndex, startIndex + data.length ) );
   }
};

</script>