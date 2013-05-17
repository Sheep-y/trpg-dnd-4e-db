/*
 * data_search.js
 * Search data model to get a list of result.
 */

od.search = {
   /**
    * Call to do a search.  Will do a search after required lising and indices are loaded.
    * @param {Object} options Options: {
    *    category : category object, null for all categories,
    *    term : terms to search for,
    *    search_body : if false, search listing only, otherwise search both listing and content,
    *    onblank: callback if search term is blank or invalid,
    *    ondone : callback after search is finished with search result { columns, list }, which may be zero rows
    * }
    */

   "search" : function search_search ( options ) {
      var tmp = options.term ? this.gen_search( options.term ) : null;
      if ( ! tmp ) {
         _.call( options.onblank );
         return;
      }
      var regx = options.regx = tmp[0];
      options.highlight = tmp[1];
      var cat = options.category;
      if ( cat ) {
         // Search in a single category
         cat.load_listing( function(){
            if ( ! options.search_body ) {
               cat.load_index( function(){
                  do_search( );
               });
            } else {
               do_search( );
            }
         });
      } else {
         // Search in all categories
         od.data.load_all_listing( function(){
            if ( options.search_body ) {
               od.data.load_all_index( function(){
                  do_search( );
               });
            } else {
               // Seach listing only
               do_search( );
            }
         });
      }

      function do_search () {
         var list = { "columns":[], "data":[] };
         var search_body = options.search_body;
         if ( cat ) {
            _.info( 'Search ' + cat.name + ': ' + options.term );
            list.columns = cat.columns;
            list.data = [].concat( search( cat.list ) );
            _.call( options.ondone, null, od.search.normalise_list( list ) );
         } else {
            _.info( 'Searching all categories: ' + options.term  );
            list.columns = ["ID","Name","Category","Type","Level","SourceBook"];
            list.data = [];
            od.data.get().forEach( function( c ) {
               list.data = list.data.concat( search( c.list ) );
            } );
            _.call( options.ondone, null, od.search.normalise_list( list ) );
         }
         
         function search( lst ) {
            var result = [];
            for ( var i = 0, l = lst.length ; i < l ; i++ ) {
               var row = lst[i];
               for ( var prop in row ) {
                  if ( prop !== '_category' ) {
                     if ( row[prop].raw ) {
                        if ( regx.test( row[prop].raw ) ) {
                           result.push( row );
                           break;
                        }
                     } else {
                        if ( regx.test( row[prop] ) ) {
                           result.push( row );
                           break;
                        }
                     }
                     if ( search_body ) {
                        if ( regx.test( row._category.index[row["ID"]] ) ) {
                           result.push( row );
                           break;
                        }
                     }
                  }
               }
            }
            return result;
         }
      };
   },

   "list_category" : function data_search_list_category ( cat, onload ) {
      od.data.load_catalog( function data_search_list_category_load() {
         var list = { "columns":[], "data":[] };
         if ( cat ) {
            _.info( 'List ' + cat.title );
            cat.load_listing( function data_search_list_category_load_cat() {
               list.columns = cat.columns;
               list.data = [].concat( cat.list );
               _.call( onload, cat, od.search.normalise_list( list ) );
            });
         } else {
            _.info( 'List all categories' );
            list.columns = ["ID","Name","Category","Type","Level","SourceBook"];
            od.data.load_all_listing( function data_search_list_category_load_all(){
               od.data.get().forEach( function data_search_list_category_each( c ) {
                  list.data = list.data.concat( c.list );
               } );
               _.call( onload, null, od.search.normalise_list( list ) );
            });
         }
      } );
   },

   /**
    * Given data.columns and data.data, convert data.data to two-dimension array that match the columns.
    * 
    * @param {Object} data before conversion: { "columns": ["col1","col2"], "data": [ {"col1":xxx}, {"col2":bbb,"col3":ddd}, ... ] }
    * @returns {Object} data after conversion: { "columns": ["col1","col2"], "data": [ [xxx, null], [null, bbb], ... ] }
    */
   "normalise_list" : function data_search_normalise_list( data ) {
      var list = data.data;
      var col = data.columns;
      var cl = col.length;
      _.debug( 'Normalise ' + list.length + ' results' );
      for ( var i = 0, l = list.length ; i < l ; i++ ) {
         var item = [];
         var base = list[i];
         for ( var c = 0 ; c < cl ; c++ ) {
           item.push( base[col[c]] ? base[col[c]] : null );
         }
         item._category = base._category.name;
         list[i] = item;
      }
      return data;
   },

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Given a textual search term, return a regular expression and highlight terms.
    *
    * Syntax: Terms are separated by space, and can be double quoted.
    *         Terms with leading '-' are excluded, otherwise are searched in any order, case in-sensitively.
    *         The word "OR" (uppercase, without quote) can be used to specify an or condition between left and right terms.
    *         Terms are not searched word by word, so for example they can appears in the middle of a word.
    *
    * Example : javascript OR ecmascript "bug database"
    *
    * @param {String} term  Terms to search for
    * @return {Array} [ RegExp, ["highlight 1", "highlight 2", ... ] ] OR null (if terms turn out to be empty conditions)
    */
   "gen_search" : function search_gen_search ( term ) {
      var hl = [];
      var regx = "^";
      var parts = term.match( /[+-]?(?:"[^"]+"|\S+)/g );
      if ( ! parts ) return null;

      while ( parts.length > 0 && parts[0] === 'OR' ) parts.splice(0,1);
      var l = parts.length;
      while ( l > 0 && parts[l-1] === 'OR' ) parts.splice(--l,1);

      for ( var i = 0, l = parts.length ; i < l ; ) {
         var addPart = [];
         do {
            var term = parts[i];
            var part = "";
            if ( term.indexOf('-') === 0 ) {
               term = term.substr(1);
               part += '(?!.*';
            } else {
               if ( term.indexOf('+') === 0 ) term = term.substr(1);
               part += '(?=.*';
            }
            if ( term ) {
               if ( /^[+-]?"[^"]+"$/.test( term ) ) {
                  term = term.substr( 1, term.length-2 );
                  if ( term ) part += term;
                  else part = "";
               } else {
                  part += term;
               }
               if ( part ) {
                  if ( part.indexOf( '(?=' ) === 0 ) hl.push( term );
                  part += '.*)';
                  addPart.push( part );
               }
            }
            i++;
            if ( i >= l || parts[i] !== 'OR' ) break;
            do { ++i; } while ( i < l && parts[i] === 'OR' );
         } while ( i < l )
         if ( addPart.length ) {
            if ( addPart.length === 1 ) {
               regx += addPart[0];
            } else {
               regx += '(?:' + addPart.join('|') + ')';
            }
         }
      }
      if ( regx === '^' ) return null;
      return [ new RegExp( regx, 'i' ), hl ];
   }
};