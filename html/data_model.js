/*
 * data_model.js
 *
 * Model and manage data in memory.
 * Data model does not care whether it is dirty; that is handled by updater.
 */

(function() {

od.data = {
   /* Please do NOT access directly. */
   "category" : {},
   /* Read only, don't modify. */
   "index" : null,

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

   "category_name_of" : function data_category_name_of ( id ) {
      var cat = id.split( /\d/, 2 )[0];
      switch ( cat ) {
         case 'associate': return 'companion';
         case 'skill'    : return 'glossary';
         case 'terrain'  : return 'trap';
      }
      return cat.toLowerCase();
   },

   "list" : function data_list () { return Object.keys( this.category ); },

   "load_catalog" : function data_load_catalog ( ondone, onerror ) {
      od.reader.read_catalog( ondone, onerror );
   },

   "load_name_index" : function data_name_index ( ondone ) {
      var countdown = 2;
      var callback = function name_countdown(){ if ( --countdown === 0 ) ondone(); };
      od.reader.read_catalog( callback );
      od.reader.read_name_index( callback );
   },

   "load_all_index" : function data_load_all_index ( ondone ) {
      var countdown = this.list().length;
      var callback = function index_countdown(){ if ( --countdown === 0 ) ondone(); };
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_index( callback );
   },

   "load_all_listing" : function data_load_all_listing ( ondone ) {
      var countdown = this.list().length;
      var callback = function listing_countdown(){ if ( --countdown === 0 ) ondone(); };
      var cats = this.category;
      for ( var cat in cats ) cats[cat].load_listing( callback );
   }
};

od.data.Category = function Category ( name ) {
   this.name = name;
   this.count = 0;
   this.raw_list = [];
   this.index = {};
   this.columns = [];
   this.list = [];
   this.map = {};
   this.data = {};
};
od.data.Category.prototype = {
   "name": "",
   "count": 0,
   "raw_list": [],     // e.g. [ ["sampleId001","Sample",["1+",1,3],["Multiple","Git","Csv"]], ... ]
   "columns": [], // e.g. [ "Name","SourceBook","Level", ... ]
   "list" : [],   // e.g. [ {ID:"sampleId001", SourceBook": { "text":"Multiple", "set": ["Git","Csv"] }, ... ]
   "index": {},   // e.g. { "sampleId001":"Sample Data 1 Published in ...", ... }
   "map" : {},   // e.g. { "sampleId001": (point to same item in list), ... }
   "data" : {},   // e.g. { "sampleId001": "<h1 class='player'>Sample Data 1</h1><p class='flavor'>..." }, ... }

   /**
    * Return localised title/
    * @returns {_L8.data_Cat_unload}
    */
   "getTitle" : function data_Cat_getTitle() {
      return _.l( 'data.category.' + this.name, _.ucfirst( this.name ) );
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

   // Build this.columns and this.list.
   "build_listing" : function data_Cat_bulid_listing () {
      var data = this.raw_list;
      var col = this.columns;
      var map = this.map = {};
      var list = this.list = new Array( data.length );

      var catName = _.ucfirst( this.name ), typeCol = 2;
      if ( [ 'Race', 'Paragonpath', 'Epicdestiny' ].indexOf( catName ) >= 0 ) {
            catName = _.ucword( catName.replace( /(?=path$|destiny$)/, ' ' ) ); // Split PP/ED into two words
         typeCol = 0;
      }

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
         item._CatName = catName;
         item._TypeName = typeCol ? item[ col[ typeCol ] ] : '';
         list[ i ] = item;
         map[ listing[ 0 ] ] = item;
      }
   }
};

})();