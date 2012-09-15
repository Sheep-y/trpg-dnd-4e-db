<script>'use strict';
/*
 * data_read.js
 * Read data from persistent storage
 */

 oddi.reader = {
    read_index: function reader_read_index( version, data ) {
       oddi.data.category = data;
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