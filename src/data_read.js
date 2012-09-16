<script>'use strict';
/*
 * data_read.js
 * Read data from persistent storage
 */

 oddi.reader = {
    _loaded: {},
    _timeout: 1 * 1000,

    read_index: function reader_read_index( version, data ) {
       var src = 'data/index.jsonp';
       if ( ! version ) {
          this._loaded[src] = setTimeout(function(){
             // timeout
             alert('timeout');
          }, this._timeout);
          _.js(src, function(a){ if ( oddi.reader._loaded[src] ) {
             // error
             clearTimeout( this._loaded[src] );
             alert('error');
          }});
       } else {
          oddi.data.category = data;
          clearTimeout( this._loaded[src] );
       }
    },
    read_data_listing: function reader_read_data_listing( version, category, columns, data ) {
       oddi.data.data[category] = {
          columns: columns,
          listing: data,
          index: {},
          data: [],
       }
    },
    read_data_index: function reader_read_data_index( version, category, data ) {
       oddi.data.data[category].index = data;
    },
    read_data: function reader_read_data( version, category, startIndex, data ) {
       var ary = oddi.data.data[category].data;
       for ( var i = 0 ; i <= 99 ; i++ ) {
          ary[startIndex+i] = data[i];
       }
    }
 }
</script>