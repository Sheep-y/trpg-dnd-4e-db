<script>'use strict';
/*
 * data_reader.js
 * Read data from persistent storage
 */

oddi.reader = {
   _loaded: {},
   _timeout: 1 * 1000,

   /**
      * Call _.js if onload is empty, or onread if it is not empty.
      * Shows errmsg if anything happens.
      */
   _read: function reader_read( doload, src, onread, errmsg ) {
      if ( ! doload ) {
          this._loaded[src] = setTimeout(function(){
            // timeout
            this._read_cleanup( src, errmsg );
         }, this._timeout);
         _.js(src, function(a){ if ( oddi.reader._loaded[src] ) {
            // error
            this._read_cleanup( src, errmsg );
         }});
      } else {
         if ( onread ) onread();
         this._read_cleanup( src );
      }
   },

   _read_cleanup: function reader_read_cleanup( src, errmsg ) {
      clearTimeout( this._loaded[src] );
      this._loaded[src] = 0;
      if ( errmsg ) alert( errmsg );
   },

   read_index: function reader_read_index( version, data, errmsg ) {
      oddi.reader._read( version, 'data/index.jsonp', function(){
         oddi.data.category = data;
      }, errmsg);
   },

   read_data_listing: function reader_read_data_listing( category, version, columns, data, errmsg ) {
      oddi.reader._read( version, 'data/'+category+'/listing.jsonp', function(){
         oddi.data.data[category] = {
            columns: columns,
            listing: data,
            index: {},
            data: [],
         }
      }, errmsg );
   },

   read_data_index: function reader_read_data_index( category, version, data, errmsg ) {
      oddi.reader._read( version, 'data/'+category+'/index.jsonp', function(){
         oddi.data.data[category].index = data;
      }, errmsg );
   },

   read_data: function reader_read_data( category, index, version, data, errmsg ) {
      startIndex = Math.floor( index / 100 );
      endIndex = startIndex + 99;
      oddi.reader._read( version, 'data/'+category+'/data'+startIndex+'-'+endIndex+'.jsonp', function(){
         var ary = oddi.data.data[category].data;
         for ( var i = 0 ; i <= 99 ; i++ ) {
            ary[startIndex+i] = data[i];
         }
      }, errmsg );
   }
}
</script>