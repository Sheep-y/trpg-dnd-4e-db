/**
 * data_reader.js
 *
 * Read data from files written by data_writter.
 */

od.reader = {

   /**
     * Internal routine; add jsonp call and timeout detection
     */
   _read: function reader_read ( src, validate, onload, onerror ) {
      var errorHandler = onerror;
      if ( onerror && typeof( onerror ) === 'string' ) errorHandler = function(){ _.alert( onerror ); };
      _.js({
         "url"     : src,
         "validate": validate,
         "onload"  : onload,
         "onerror" : errorHandler });
   },

   /**
    *  Internal routine: decompress compressed data.
    */
   _inflate: function reader_inflate ( version, name, data ) {
      if ( version === 20170324 ) try {
         _.time( '[Reader] Decompressing ' + name );
         data = Base85.decode( data );
         data = LZMA.decompress( data ); // Heaviest step
         data = JSON.parse( data );
         _.time( '[Reader] Decompressed ' + name );
      } catch ( err ) {
         /* if ( err instanceof SyntaxError ) document.body.textContent = data; */
         throw err;
      }
      return data;
   },

   /////////////////////////////////////////////////////////

   // Catalog is the main listing of saved data - a list of catagories and number of items

   read_catalog: function reader_read_catalog( onload, onerror ) {
      var path = od.config.url.catalog();
      this._read(
        path,
        // Detecting data length may be insufficient to know whether catalog is really loaded, e.g. loaded single data.
        function(){ return od.reader.read_catalog.read && od.data.list().length > 0; },
        onload,
        onerror ? onerror : 'Cannot read data catalog from ' + path );
   },

   jsonp_catalog: function reader_jsonp_catalog( version, data ) {
      for ( var cat in data ) od.data.create( cat ).count = data[cat];
      od.reader.read_catalog.read = true;
   },

   /////////////////////////////////////////////////////////

   // Name index is a map of name to id, for creation of internal links

   read_name_index: function reader_read_index( onload, onerror ) {
      var path = od.config.url.index();
      this._read(
        path,
        function(){ return od.data.index; },
        onload,
        onerror ); // Ignore error since it is not critical
   },

   jsonp_name_index: function reader_jsonp_index( version, data ) {
      od.data.index = od.reader._inflate( version, "name index", data );
   },

   /////////////////////////////////////////////////////////

   // Listing is a processed listing of items in a category.

   read_data_listing: function reader_read_data_listing( category, onload, onerror ) {
      var path = od.config.url.listing( category );
      this._read(
         path,
         function(){ return od.data.get(category).list.length > 0; },
         onload,
         onerror ? onerror : 'Cannot read ' + category + ' listing from ' + path );
      // TODO: Make error handler use thrown error message (e.g. need reindex) instead of default
   },

   jsonp_data_listing: function reader_jsonp_data_listing( version, category, columns, data ) {
      if ( version < 20130703 || version === 20140414 )
         return _.alert( _.l( 'error.old_format' ) );
      var cat = od.data.get( category );
      cat.columns = columns;
      cat.list = od.reader._inflate( version, "listing", data );
      cat.build_listing();
   },

   jsonp_data_extended: function reader_jsonp_data_extended( version, category, columns, data ) {
      return _.alert( _.l( 'error.old_format' ) ); // Called by 3.0 Milestone 1 data (ver 20130330)
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
      if ( version < 20130616 )
         return _.alert( _.l( 'error.old_format' ) );
      var cat = od.data.get( category );
      cat.index = od.reader._inflate( version, "text index", data );
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
      return _.alert( _.l( 'error.old_format' ) ); // Old unbatched data
   },

   jsonp_batch_data: function reader_jsonp_batch_data( version, category, data ) {
      if ( version < 20160803 )
         return _.alert( _.l( 'error.old_format' ) );
      var cat = od.data.get(category);
      for ( var id in data )
         cat.data[id] = data[id];
   }
};