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
     * 
     * Workflow:
     *  > reader.read_XXX()
     *  >> reader._read( undefined, src );
     *  >>> setTimeout
     *  >>> _.js( src )
     *  > reader.read_XXX( version, data ) // Callback from js
     *  >> reader._read( version, src, onread );
     *  >>> onread
     *  >>> cleanup
     */
   _read: function reader_read( doload, src, onread, errmsg ) {
      var reader = oddi.reader;

      function cleanup( errmsg ) {
         reader._loaded[src] = clearTimeout( reader._loaded[src] );
         if ( errmsg ) _.error( errmsg );
      }

      if ( ! doload ) {
        // Straight call, load file

        // Timeout detection
        reader._loaded[src] = setTimeout( function(){ cleanup( errmsg ); }, reader._timeout);
         _.js(src, function(a){
            // Onload. Normally callback below should clear the timer, if not then we have error.
            if ( oddi.reader._loaded[src] ) cleanup( errmsg );
        });
      } else {
         // Callback from file
         if ( onread ) onread();
         cleanup( );
      }
   },

   read_index: function reader_read_index( version, data ) {
      oddi.reader._read( version, 'data/index.jsonp', function(){
         oddi.data.category = data;
      }, 'Cannot read data' );
   },

   read_data_listing: function reader_read_data_listing( category, version, columns, data ) {
      oddi.reader._read( version, 'data/'+category+'/listing.jsonp', function(){
         oddi.data.data[category] = {
            columns: columns,
            listing: data,
            index: {},
            data: [],
         }
      }, 'Cannot read listing of '+category );
   },

   read_data_index: function reader_read_data_index( category, version, data ) {
      oddi.reader._read( version, 'data/'+category+'/index.jsonp', function(){
         oddi.data.data[category].index = data;
      }, 'Cannot read index of '+category );
   },

   read_data: function reader_read_data( category, index, version, data ) {
      startIndex = Math.floor( index / 100 );
      endIndex = startIndex + 99;
      oddi.reader._read( version, 'data/'+category+'/data'+startIndex+'-'+endIndex+'.jsonp', function(){
         var ary = oddi.data.data[category].data;
         for ( var i = 0 ; i <= 99 ; i++ ) {
            ary[startIndex+i] = data[i];
         }
      }, 'Cannot read data of '+category );
   }
}
</script>