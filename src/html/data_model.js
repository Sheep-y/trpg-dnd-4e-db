/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * data_model.js
 *
 * Model and manage data in memory.
 * Data model does not care whether it is dirty; that is handled by updater.
 */

(function() {

od.data = {
   /* Please do NOT access directly. */
   "category" : {},

   /** Clear all loaded data or a category of data */
   "clear" : function data_clear ( cat ) {
      var category = this.category;
      for ( var c in category ) {
         if ( cat === undefined || cat === c ) {
            category[c].unload();
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
   "get" : function data_get ( name ) {
      var result;
      if ( name === undefined ) {
         result = [];
         for ( var c in this.category ) result.push( this.category[c] );
         return result;
      }
      result = this.category[name];
      return result ? result : null;
   },

   /**
    * Make sure a category exists and return it.
    *
    * @param {type} name Name of category.
    * @returns {od.data.Category} Requested category.
    */
   "create" : function data_create ( name ) {
      var result = this.category[name];
      if ( result === undefined ) {
         this.category[name] = result = new od.data.Category( name );
      }
      return result;
   },

   "list" : function data_list () { return Object.keys( this.category ); },

   "load_catalog" : function data_load_catalog ( ondone, onerror ) { od.reader.read_catalog( ondone, onerror ); },

   "load_all_index" : function data_load_all_index ( ondone ) {
      var lat = new _.Latch( this.list().length+1, ondone );
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_index( lat.count_down_function() );
      lat.count_down();
   },

   "load_all_listing" : function data_load_all_listing ( ondone ) {
      var lat = new _.Latch( this.list().length+1, ondone );
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_listing( lat.count_down_function() );
      lat.count_down();
   },

   "save_catalog" : function data_save_catalog ( ondone, onerror ) {
      od.data.get().forEach( function(e) {
         if ( e.count === 0 ) delete od.data.category[e.name];
      });
      od.writer.write_catalog( ondone, onerror );
   },

   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    *
    * @param {String} data Raw entry data to be processed.
    */
   "preprocess" : function data_preprocess ( data ) {
      // Normalise input
      data = data.trim().replace( /\r\n?/g, '\n' );
      // Extract body
      if ( data.indexOf( '<body' ) >= 0 ) {
         data = data.match( /<body[^>]*>\s*((?:.|\n)*?)\s*<\/body\s*>/ );
         if ( !data ) return null;
         data = data[1].trim();
      }
      // Remove script and form tags
      data = data.replace( /<script\b.*?<\/script\s*>/g, '' ).replace( /<\/?(input|form)[^>]*>/g, '');
      // Normalise whitespace
      data = data.replace( /[\n\s]+/g, ' ' );
      // Remove empty contents
      data = data.replace( /<div[^>]*>\s*<\/div\s*>/g, '' );
      // Compression - Removes quotes, links, nbsp etc.
      data = data.replace( /(<\w+ \w+)\s*=\s*"([^" ]+)"/g, '$1=$2' );
      data = data.replace( /(<\w+ \w+)\s*=\s*'([^' ]+)'/g, '$1=$2' );
      data = data.replace( /<br\s*\/>/g, '<br>' );
      data = data.replace( /&nbsp;/g, '\u00A0' );
      data = data.replace( /<\/?a(\s[^>]+)?>/g, '' );
      data = data.replace( / alt=""/g, ' ' );
      // Image conversion
      data = data.replace( /https?:\/\/(www\.)?wizards\.com\/dnd\/images\//g, 'images/' );
      data = data.replace( /<img [^>]*src=images\/bullet\.gif [^>]*\/>/g, '✦' ); // Most common symbol at 100k.
      data = data.replace( /<img [^>]*src=images\/symbol\/x\.gif [^>]*\/>/g, '✦' ); // Second most common symbol at 40k.
      data = data.replace( /<img [^>]*src=images\/symbol\/aura\.png [^>]*\/>/g, '☼' ); // About 1000 hits
      // S1 - basic close ; S2 - basic melee ; S3 - basic ranged ; S4 - basic area. Should use \u20DD Enclosing circle but only supported by Code2000, too high requirment
      // Z1 & z1a - close ; z2a - melee ; z3a - ranged ; Z4 & z4a - area
      data = data.replace( /<img [^>]*src=images\/symbol\/[sS]1\.gif [^>]*\/>/g, '͜͡⋖' ); // None
      data = data.replace( /<img [^>]*src=images\/symbol\/[zZ]1[aA]?\.gif [^>]*\/>/g,'⋖' ); // ~3300
      data = data.replace( /<img [^>]*src=images\/symbol\/[sS]2\.gif [^>]*\/>/g, '͜͡⚔' ); // ~5600
      data = data.replace( /<img [^>]*src=images\/symbol\/[zZ]2[aA]?\.gif [^>]*\/>/g, '⚔' ); // ~4200
      data = data.replace( /<img [^>]*src=images\/symbol\/[sS]3\.gif [^>]*\/>/g, '͜͡➶' ); // ~950
      data = data.replace( /<img [^>]*src=images\/symbol\/[zZ]3[aA]?\.gif [^>]*\/>/g, '➶' ); // ~2000
      data = data.replace( /<img [^>]*src=images\/symbol\/[sS]4\.gif [^>]*\/>/g, '͜͡✻' ); // No hits
      data = data.replace( /<img [^>]*src=images\/symbol\/[zZ]4[aA]?\.gif [^>]*\/>/g, '✻' ); // ~720
      // 1a ... 6a = dice face 1-6
      data = data.replace( /<img [^>]*src=images\/symbol\/1[aA]\.gif [^>]*\/>/g, '⚀' ); // 1
      data = data.replace( /<img [^>]*src=images\/symbol\/2[aA]\.gif [^>]*\/>/g, '⚁' ); // 4
      data = data.replace( /<img [^>]*src=images\/symbol\/3[aA]\.gif [^>]*\/>/g, '⚂' ); // ~30
      data = data.replace( /<img [^>]*src=images\/symbol\/4[aA]\.gif [^>]*\/>/g, '⚃' ); // ~560
      data = data.replace( /<img [^>]*src=images\/symbol\/5[aA]\.gif [^>]*\/>/g, '⚄' ); // ~2100
      data = data.replace( /<img [^>]*src=images\/symbol\/6[aA]\.gif [^>]*\/>/g, '⚅' ); // ~2500
      // Convert ’ to ' so that people can actually search for it
      data = data.replace( /’/g, "'" );
      return data.trim();
   },

   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    *
    * @param {String} data Data to be indexed.
    */
   "indexify" : function data_indexify ( data ) {
      // Remove power and item flavors
      data = data.replace( /(<h1 class=\w+power><span[^>]+>[^<]+<\/span>[^<]+<\/h1>)<p class=flavor>.*?<\/p>/g, '$1' );
      data = data.replace( /<p class=miflavor>.*?<\/p>/g, '' );
      // Remove tags and condense whitespaces
      data = data.replace( /<[^>]+>/g, ' ' );
      data = data.replace( /  +/g, ' ' );
      return data.trim();
   }
};

od.data.Category = function Category ( name ) {
   this.name = name;
   this.unload();
};
od.data.Category.prototype = {
   "name": "",
   "title": "",
   "count": 0,

   /** Raw data used to compose list property */
   "raw_columns": [],  // e.g. ["ID","Name","SourceBook", ... ]
   "raw": [],          // e.g. [ ["sampleId001","Sample Data 1","Git,Csv"], ... ]
   "ext_columns": [],  // e.g. ["ID","Name","Level","SourceBook", ... ]
   "extended": [],     // e.g. [ ["sampleId001","Sample",["1+",1,3],["Multiple","Git","Csv"]], ... ]
   "index": {},        // e.g. { "sampleId001":"Sample Data 1 Published in ...", ... }

   "columns": [], // e.g. [ Name","SourceBook","Level", ... ]
   "list" : [],   // e.g. [ {ID:"sampleId001", SourceBook": { "text":"Multiple", "set": ["Git","Csv"] }, ... ]
   "map" : {},   // e.g. { "sampleId001": (point to same item in list), ... }
   "data" : {},   // e.g. { "sampleId001": "<h1 class='player'>Sample Data 1</h1><p class='flavor'>..." }, ... }

   /**
    * Return localised title/
    * @returns {_L8.data_Cat_unload}
    */
   "getTitle" : function data_Cat_getTitle() {
      return _.l( 'data.category.' + this.name, this.name );
   },

   /**
    * Clear all loaded data and columns.
    *
    * @returns undefined
    */
   "unload" : function data_Cat_unload () {
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

   "load_raw" : function data_Cat_load_raw ( ondone, onerror ) {
      od.reader.read_data_raw( this.name, ondone, _.callonce( onerror ) );
   },

   "load_listing" : function data_Cat_load_listing ( ondone, onerror ) {
      var cat = this;
      od.reader.read_data_listing( this.name, function data_Cat_load_listing_done() {
         if ( cat.list.length <= 0 ) cat.build_listing(); // Skip if listing has been built
         _.call( ondone );
      }, _.callonce( onerror ) );
   },

   "load_index" : function data_Cat_load_index ( ondone, onerror ) {
      od.reader.read_data_index( this.name, ondone, onerror );
   },

   "load_data" : function data_Cat_load_data ( id, ondone, onerror ) {
      od.reader.read_data( this.name, id, ondone, onerror );
   },

   "save" : function data_Cat_save_listing ( ondone, onerror ) {
      // od.reader.jsonp_data_listing( 20120915, "Sample", [ "Id", "Name", "Category", "SourceBook" ], [
      var l = new _.Latch( 3, ondone ), countdown = l.count_down_function();
      onerror = _.callonce( onerror );
      od.writer.write_data_raw( this, countdown, onerror );
      od.writer.write_data_listing( this, countdown, onerror );
      od.writer.write_data_index( this, countdown, onerror );
   },

   "save_data" : function data_Cat_save_data ( id, ondone, onerror ) {
      od.writer.write_data( this, id, this.data[id], ondone, onerror );
   },

   "check_columns" : function data_Cat_check_columns ( col ) {
      if ( JSON.stringify(this.raw_columns) !== JSON.stringify(col) ) {
         this.unload();
         this.count = 0;
         this.raw_columns = col;
         this.ext_columns = this.parse_extended( col, null );
      }
   },

   "parse_extended" : function data_Cat_parse_extended ( listing, data ) {
      var result = listing.concat();
      // Data is null = listing columns
      if ( data ) {
         result[ 0 ] = od.config.id( result[ 0 ] );
         var pos = this.ext_columns.indexOf( 'SourceBook' );
         if ( pos && listing[ pos ] && listing[ pos ].indexOf(',') >= 0 ) result[ pos ] = [ listing[pos] ].concat( listing[pos].split(',') );
      }
      return result;
   },

   "update" : function data_Cat_update ( id, listing, data, i ) {
      id = od.config.id( id );
      if ( i === undefined ) i = _.col( this.raw ).indexOf( id );
      data = od.data.preprocess( data );
      if ( i < 0 ) i = this.count++;
      this.raw[i] = listing;
      this.extended[i] = this.parse_extended( listing, data );
      this.index[id] = od.data.indexify( data );
      this.data[id] = data;
   },

   // Build this.columns and this.list.
   "build_listing" : function data_Cat_bulid_listing () {
      var data = this.extended;
      var col = this.ext_columns;

      this.columns = col.concat();
      var list = this.list = new Array( data.length );
      var map = this.map = {};

      var colCount = col.length;
      for ( var i = 0, l = data.length ; i < l ; i++ ) {
         var listing = data[i];
         var item = {};
         for ( var j = 0 ; j < colCount ; j++ ) {
            var prop = listing[ j ];
            if ( ! prop || typeof( prop ) === 'string' ) {
               item[ col[ j ] ] = prop;
            } else {
               item[ col[ j ] ] = { "text" : prop[ 0 ], "set" : prop.slice( 1 ) };
            }
         }
         item._category = this;
         list[ i ] = item;
         map[ listing[ 0 ] ] = item;
      }
   }
};
_.seal( od.data.Category.prototype );

})();