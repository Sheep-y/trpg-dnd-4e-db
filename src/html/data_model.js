/*
 * data_model.js
 * Model and manage data in memory.
 * Data model does not care whether it is dirty; that is handled by downloader.
 */

(function() {

od.data = {
   /* Please do NOT access directly. */
   "category" : {},

   /** Clear all loaded data or a category of data */
   "clear" : function data_clear( cat ) {
      var category = this.category;
      for ( var c in category ) {
         if ( cat === undefined || cat === c ) {
            category[c].clear;
            delete category[c];
         }
      }
   },

   /**
    * With no parameter: return an array of category names.
    * With string parameter: Return a category.
    * 
    * @param {type} name Name of category.
    * @returns {od.data.Category} Requested category. Null if category not exist.
    */
   "get" : function data_get( name ) {
      if ( name === undefined ) {
         var result = [];
         for ( var c in this.category ) result.push( this.category[c] );
         return result;
      }
      var result = this.category[name];
      return result ? result : null;
   },

   /**
    * Make sure a category exists and return it.
    * 
    * @param {type} name Name of category.
    * @returns {od.data.Category} Requested category.
    */
   "create" : function data_get( name ) {
      var result = this.category[name];
      if ( result === undefined ) {
         this.category[name] = result = new od.data.Category( name );
      }
      return result;
   },

   "list" : function data_list() { return Object.keys( this.category ); },

   "load_catalog" : function data_load_catalog ( ondone, onerror ) { od.reader.read_catalog( ondone, onerror ); },

   "load_all_index" : function data_load_all_index( ondone ) {
      var lat = new _.Latch( this.list().length+1, ondone );
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_index( lat.count_down_function() );
      lat.count_down();
   },

   "load_all_listing" : function data_load_all_listing( ondone ) {
      var lat = new _.Latch( this.list().length+1, ondone );
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_listing( lat.count_down_function() );
      lat.count_down();
   },
           
   "save_catalog" : function data_save_catalog( ondone, onerror ) {
      od.data.get().forEach( function(e) {
         if ( e.count === 0 ) delete od.data.category[e.name];
      });
      od.writer.write_catalog( ondone, onerror );
   },

   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    */
   "preprocess" : function data_preprocess( data ) {
      // Normalise input
      data = data.trim().replace( /\r\n?/g, '\n' );
      // Extract body
      data = data.match( /<body[^>]*>\s*((?:.|\n)*?)\s*<\/body\s*>/ );
      if ( !data ) return null;
      // Remove script and form tags
      data = data[1].trim().replace( /<script\b.*?<\/script\s*>/g, '' ).replace( /<input[^>]*>/g, '').replace( /<form[^>]*>|<\/form\s*>/g, '' );
      // Normalise whitespace
      data = data.replace( /[\n\s]+/g, ' ' );
      // Remove empty contents
      data = data.replace( /<div[^>]*>\s*<\/div\s*>/g, '' );
      // Compression - Removes quotes, links, nbsp etc.
      data = data.replace( /(<\w+ \w+=)"([^" ]+)"/g, '$1$2' );
      data = data.replace( /<br\s*\/>/g, '<br>' );
      data = data.replace( /&nbsp;/g, '\u00A0' );
      data = data.replace( /<\/?a(\s[^>]+)?>/g, '' );
      // Convert ’ to ' so that people can actually search for it
      data = data.replace( /’/g, "'" );
      return data.trim();
   },
           
   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    */
   "indexify" : function data_indexify( data ) {
      // Remove tags
      data = data.replace( /<p class=flavor>.*?<\/p>/g, '' );
      data = data.replace( /<[^>]+>/g, ' ' );
      data = data.replace( /\s+/g, ' ' );
      return data.trim();
   }
};

od.data.Category = function Category( name ) {
   this.name = name;
   this.title = _.l( 'data.category.' + name, name );
   this.unload();
};
od.data.Category.prototype = {
   "name": "",
   "title": "",
   "count": 0,

   /** Raw data used to compose list property */
   "raw_columns": [],  // e.g. ["ID","Name","Category","SourceBook", ... ]
   "raw": [],          // e.g. [ ["sampleId001","Sample Data 1","Sample1","Git"], ... ]
   "ext_columns": [],  // e.g. ["ID","Level","SourceBook", ... ]
   "extended": [],     // e.g. [ ["sampleId001",[1,3],["PHB","PHB2"]], ... ]
   "index": {},        // e.g. { "sampleId001":"Sample Data 1 Published in ...", ... }

   "columns": [], // e.g. [ "ID","Name","Category","SourceBook","Level", ... ]
   "list" : [],   // e.g. [ {ID:"sampleId001", "SourceBook": { "raw":"Multiple", "ext": ["PHB","PHB2"] }, ... ]
   "map" : {},    // e.g. { "sampleId001": (point to same item in list), ... }
   "data" : {},   // e.g. { "sampleId001": "<h1 class='player'>Sample Data 1</h1><p class='flavor'>..." }, ... }

   /**
    * Clear all loaded data and columns.
    * 
    * @returns undefined
    */
   "unload" : function data_Cat_unload() {
      if ( this.list ) this.count = this.list.length;
      this.raw_columns = [];
      this.raw = [];
      this.ext_columns = [];
      this.extended = [];
      this.index = {};
      this.columns = [];
      this.list = [];
      this.map = {};
      this.data = {};
   },

   "load_listing" : function data_Cat_load_listing( ondone, onerror ) {
      var cat = this;
      var lat = new _.Latch( 3 );
      var err = onerror ? function(){ onerror.call(); onerror = function(){}; } : onerror;
      od.reader.read_data_raw( this.name, lat.count_down_function(), err );
      od.reader.read_data_extended( this.name, lat.count_down_function(), err );
      lat.ondone = function(){
         cat.build_listing();
         _.call( ondone );
      };
      lat.count_down();
   },

   "load_index" : function data_Cat_load_index( ondone, onerror ) {
      od.reader.read_data_index( this.name, ondone, onerror );
   },

   "load_data" : function data_Cat_load_data( id, ondone, onerror ) {
      od.reader.read_data( this.name, id, ondone, onerror );
   },
           
   "save_listing" : function data_Cat_save_listing( ondone, onerror ) {
      // od.reader.jsonp_data_listing( 20120915, "Sample", [ "Id", "Name", "Category", "SourceBook" ], [
      var l = new _.Latch( 2, ondone );
      onerror = _.callonce( onerror );
      od.writer.write_data_listing ( this, l.count_down_function(), onerror );
      od.writer.write_data_extended( this, l.count_down_function(), onerror );
      
   },
           
   "save_index" : function data_Cat_save_index( ondone, onerror ) {
      od.writer.write_data_index( this, ondone, onerror );
   },

   "save_data" : function data_Cat_save_data( id, ondone, onerror ) {
      od.writer.write_data( this, id, this.data[id], ondone, onerror );      
   },

   "check_columns" : function data_Cat_check_columns( col ) {
      if ( JSON.stringify(this.raw_columns) !== JSON.stringify(col) ) {
         this.unload();
         this.count = 0;
         this.raw_columns = col;
         this.ext_columns = ["ID"];
      }
   },

   "update" : function data_Cat_update( id, listing, data ) {
      var i = _.col( this.raw ).indexOf( id );
      var cat = this;
      var id = listing[0];
      data = od.data.preprocess( data );
      var index = od.data.indexify( data );
      if ( i >= 0 ) {
         _.info('Updating ' + id + ' (' + listing[1] + ') of ' + cat.name );
         cat.load_data( id, function data_Cat_update_load(){
            cat.raw[i] = listing;
            cat.extended[i] = [ id ];
            cat.index[id] = index;
            cat.data[id] = data;
         });
      } else {
         _.info('Adding ' + id + ' (' + listing[1] + ') to ' + cat.name );
         cat.raw.push( listing );
         cat.extended.push( [ id ] );
         cat.index[id] = index;
         cat.data[id] = data;
         ++cat.count;
      }
   },

   // Build this.columns and this.list from raw listing and extended listing.
   "build_listing" : function data_Cat_bulid_listing() {
      var cat = this;
      if ( this.raw.length !== this.extended.length ) {
         _.error( _.l( 'error.wrong_ext', null, this.title ) );
         this.ext_columns = [];
         this.extended = new Array( this.raw.length );
      }

      var raw = this.raw;
      var raw_col = this.raw_columns;
      var rl = raw_col.length;

      var ext = this.extended;
      var ext_col = this.ext_columns;
      var el = ext_col.length;

      var list = this.list = [];
      var map = this.map = {};
      for ( var i = 0, ll = raw.length ; i < ll ; i++ ) {
         var item = {"Category":this.title}, r = raw[i], e = ext[i];
         for ( var j = 0 ; j < rl ; j++ ) item[raw_col[j]] = r[j];
         if ( e[0] === r[0] ) { // First item is id
            for ( var j = 1 ; j < el ; j++ ) {
               if ( item[ext_col[j]] === undefined ) {
                  item[ext_col[j]] = e[j];
               } else {
                  if ( e[j] ) item[ext_col[j]] = { "raw": item[ext_col[j]], "ext": e[j] };
               }
            }
            item._category = this;
         } else {
            _.warn( this.name + ' reindex #' + i + ': ' + e[0] + ' !== ' + r[0] );
         }
         list.push( item );
         map[ item[raw_col[0]] ] = item;
      }
      this.columns = this.raw_columns.concat( this.ext_columns );
      this.columns = this.columns.filter( function(e,i) {
         return cat.columns.indexOf(e) >= i;
      });
   }
};
_.seal( od.data.Category.prototype );

})();