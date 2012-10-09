<script type='text/javascript'>'use strict';
/*
 * data_reader.js
 * Read data from persistent storage
 */

oddi.reader = {
   _loaded: {},
   _timeout: 1 * 1000,

   /**
     * Call _.js if onload is empty, or onread if it is not empty.
     */
   _clear : function cleanup( src, onerror ) {
      clearTimeout( this._loaded[src] );
      delete this._loaded[src];
      if ( onerror ) {
         if ( typeof( onerror ) == 'function' ) onerror( src );
         else _.error( onerror );
      }
   },

   /**
      * Add jsonp call and timeout detection
      */
   _read: function reader_read( src, onload, onerror ) {
      function clear(){ oddi.reader._clear( src, onerror ); }
      this._loaded[src] = setTimeout( clear, this._timeout );
      _.js(src, function(a){
         // Onload. Normally callback should clear the timer, if not then we have error.
         if ( oddi.reader._loaded[src] ) clear();
         if ( onload ) onload();
      });
   },

   url : {
      index : function index() { return oddi.config.data_url+'/index.jsonp' },
      category_listing : function cat_listing( category ) { return oddi.config.data_url+'/'+category+'/listing.jsonp' },
      category_index : function cat_listing( category ) { return oddi.config.data_url+'/'+category+'/index.jsonp' },
      data : function data_url( category, index ) {
         startIndex = Math.floor( index / 100 );
         endIndex = startIndex + 99;
         return oddi.config.data_url+'/'+category+'/data'+startIndex+'-'+endIndex+'.jsonp';
      },
   },

   read_index: function reader_read_index( onload ) {
      this._read( this.url.index(), onload, 'Cannot read data' );
   },

   jsonp_index: function reader_jsonp_index( version, data ) {
      oddi.data.category = data;
      this._clear( this.url.index() );
   },

   read_data_listing: function reader_read_data_listing( category, onload ) {
      this._read( this.url.category_listing( category ), onload, 'Cannot read listing of '+category );
   },

   jsonp_data_listing: function reader_jsonp_data_listing( version, category, columns, data ) {
      oddi.data.data[category] = {
         columns: columns,
         listing: data,
         index: {},
         data: [],
      }
      this._clear( this.url.category_listing( category ) );
   },

   read_data_index: function reader_read_data_index( category, onload ) {
      this._read( this.url.category_index( category ), onload, 'Cannot read index of '+category );
   },

   jsonp_data_index: function reader_jsonp_data_index( version, category, data ) {
      oddi.data.data[category].index = data;
      this._clear( this.url.category_index( category ) );
   },

   read_data: function reader_read_data( category, index, onload ) {
      this._read( this.url.data( category, index ), onload, 'Cannot read data #' + index + ' of ' + category );
   },

   jsonp_data: function reader_jsonp_data( version, category, index, data ) {
      var ary = oddi.data.data[category].data;
      for ( var i = 0 ; i <= 99 ; i++ ) {
         ary[startIndex+i] = data[i];
      }
      this._clear( this.url.data( category, index ) );
   }
};

</script>