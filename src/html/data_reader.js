/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
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
      if ( onerror && typeof( onerror ) === 'string' ) errorHandler = function(){ _.error( onerror ); };
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

   // Raw is the main listing of items in a category.

   read_data_raw: function reader_read_data_raw( category, onload, onerror ) {
      var path = od.config.url.raw( category );
      this._read(
         path,
         function(){ return od.data.get(category).raw.length > 0; },
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

   read_data_listing: function reader_read_data_listing( category, onload, onerror ) {
      var path = od.config.url.listing( category );
      this._read(
         path,
         function(){ return od.data.get(category).extended.length > 0; },
         onload,
         onerror ? onerror : 'Cannot read extended ' + category + ' listing from ' + path );
      // TODO: Make error handler use thrown error message (e.g. need reindex) instead of default
   },

   jsonp_data_listing: function reader_jsonp_data_listing( version, category, columns, data ) {
      var cat = od.data.get( category );
      if ( ! cat || data.length !== cat.count || version === 20140414 ) {
         // Version 20140414 was saving compressed binary as unicode, corrupting them.
         category.count = data.length;
         _.error( _.l( 'error.inconsistent_category', 'Please re-index %1.', cat.getTitle(), 'listing' ) );
      }

      if ( version < 20130616 )
         // Milestone 1 data, file name changed, not worth supporting.
         // Can manually rename _listing.js to _raw.js and run reindex.
         return _.error( _.l( 'error.need_reget' ) );
      if ( version < 20130703 ) {
         // 20130616 format use url as id instead of simplified id
         data.forEach( function reader_jsonp_data_listing_20130616 ( item ) {
            item[ 0 ] = od.config.id( item[ 0 ] );
         } );
      }
      cat.ext_columns = columns;
      cat.extended = data;
   },

   jsonp_data_extended: function reader_jsonp_data_extended( version, category, columns, data ) {
      return _.error( _.l( 'error.need_reget' ) ); // Milestone 1 data (ver 20130330), not worth supporting. See above.
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
      if ( ! cat || Object.keys( data ).length !== cat.count || version === 20140414 ) {
         // Version 20140414 was saving compressed binary as unicode, corrupting them.
         category.count = data.length;
         _.error( _.l( 'error.inconsistent_category', 'Please re-index %1.', cat.getTitle(), 'index' ) );
      }

      if ( version < 20130703 ) {
         // 20130616 format use url as id instead of simplified id
         for ( var id in data ) {
            data[ od.config.id( id ) ] = data[ id ];
            delete data[ id ];
         }
      }
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
      if ( version < 20130703 ) {
         // 20130616 format use url as id instead of simplified id
         id = od.config.id( id );
      }
      var cat = od.data.get(category);
      cat.data[id] = data;
   }
};