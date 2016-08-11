/**
 * data_reader.js
 *
 * Read data from files written by data_writter.
 */

od.reader = {

   /**
     * Internal routine; add jsonp call and timeout detection
     */
   _read: function reader_read( src, validate, onload, onerror ) {
      var errorHandler = onerror;
      if ( onerror && typeof( onerror ) === 'string' ) errorHandler = function(){ _.alert( onerror ); };
      _.js({
         "url"     : src,
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
      od.data.index = data;
   },

   /////////////////////////////////////////////////////////

   // Listing is a processed listing of items in a category.

   read_data_listing: function reader_read_data_listing( category, onload, onerror ) {
      var path = od.config.url.listing( category );
      this._read(
         path,
         function(){ return od.data.get(category).raw_list.length > 0; },
         onload,
         onerror ? onerror : 'Cannot read ' + category + ' listing from ' + path );
      // TODO: Make error handler use thrown error message (e.g. need reindex) instead of default
   },

   jsonp_data_listing: function reader_jsonp_data_listing( version, category, columns, data ) {
      var cat = od.data.get( category );
      if ( ! cat || data.length !== cat.count || version === 20140414 ) {
         // Version 20140414 was saving compressed binary as unicode, corrupting them.
         cat.count = data.length;
         _.alert( _.l( 'error.inconsistent_category', 'Please re-export %1.', cat.getTitle(), 'listing' ) );
      }

      if ( version < 20130703 )
         return _.alert( _.l( 'error.old_format' ) );
      cat.columns = columns;
      cat.raw_list = data;
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
      var cat = od.data.get(category);
      if ( version < 20130616 )
         return _.alert( _.l( 'error.old_format' ) );
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