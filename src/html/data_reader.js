/*
 * data_reader.js
 * Read data from persistent storage
 */

od.reader = {

   /**
     * Internal routine; add jsonp call and timeout detection
     */
   _read: function reader_read( src, validate, onload, onerror ) {
      var errorHandler = onerror;
      if ( onerror && typeof( onerror ) === 'string' ) errorHandler = function(){ _.error( onerror ); };
      _.js( src, {
         "validate": validate,
         "onload"  : onload,
         "onerror" : errorHandler });
   },

   /////////////////////////////////////////////////////////

   // Catalog is the main listing of saved data - a list of catagories and number of items

   read_catalog: function reader_read_catalog( onload, onerror ) {
      var path = od.config.url.catalog();
      this._read(
        path,
        function(){ return od.data.list().length > 0; },
        onload,
        onerror ? onerror : 'Cannot read data catalog from ' + path );
   },

   jsonp_catalog: function reader_jsonp_catalog( version, data ) {
      for ( var cat in data ) od.data.create( cat ).count = data[cat];
   },

   /////////////////////////////////////////////////////////

   // Raw is the main listing of items in a category.

   read_data_raw: function reader_read_data_raw( category, onload, onerror ) {
      var path = od.config.url.raw( category );
      this._read(
         path,
         function(){ return od.data.get(category).raw.length > 0 },
         onload,
         onerror ? onerror : 'Cannot read ' + category + ' listing from ' + path );
   },

   jsonp_data_raw: function reader_jsonp_data_raw( version, category, columns, data ) {
      var cat = od.data.get(category);
      cat.raw_columns = columns;
      cat.raw = data;
   },

   /////////////////////////////////////////////////////////

   // Listing is a processed listing of items in a category.

   read_data_extended: function reader_read_data_extended( category, onload, onerror ) {
      var path = od.config.url.extended( category );
      this._read(
         path,
         function(){ return od.data.get(category).extended.length > 0; },
         onload,
         onerror ? onerror : 'Cannot read extended ' + category + ' listing from ' + path );
   },

   jsonp_data_extended: function reader_jsonp_data_extended( version, category, columns, data ) {
      var cat = od.data.get(category);
      cat.ext_columns = columns;
      cat.extended = data;
   },

   /////////////////////////////////////////////////////////

   // Index is processed full text search data of items in a category

   read_data_index: function reader_read_data_index( category, onload, onerror ) {
      var path = od.config.url.index( category );
      this._read(
         path,
         function(){ return Object.keys( od.data.get(category).index ).length > 0; },
         onload,
         onerror ? onerror : 'Cannot read ' + category + ' index from ' + path );
   },

   jsonp_data_index: function reader_jsonp_data_index( version, category, data ) {
      var cat = od.data.get(category);
      cat.index = data;
   },

   /////////////////////////////////////////////////////////

   // Data is an individual data item.

   read_data: function reader_read_data( category, id, onload, onerror ) {
      var path = od.config.url.data( category, id );
      this._read(
         path,
         function(){ return od.data.get(category).data[id] ? true : false; },
         onload,
         onerror ? onerror : 'Cannot read ' + category + '.' + id + ' from ' + path );
   },

   jsonp_data: function reader_jsonp_data( version, category, id, data ) {
      var cat = od.data.get(category);
      cat.data[id] = data;
   }
};