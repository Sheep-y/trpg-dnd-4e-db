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
      var search_body = options.search_body;
      var regx = options.regx = tmp[0];
      options.highlight = tmp[1];
      var cat = options.category;
      if ( cat ) {
         // Search in a single category
         cat.load_extended( function(){
            if ( search_body ) {
               cat.load_index( do_search );
            } else {
               do_search( );
            }
         });
      } else {
         // Search in all categories
         od.data.load_all_listing( function(){
            if ( search_body ) {
               od.data.load_all_index( do_search );
            } else {
               do_search( );
            }
         });
      }

      function do_search () {
         var list = { "columns":[], "data":[] };
         if ( cat ) {
            _.info( 'Search ' + cat.name + ': ' + options.term );
            list.columns = cat.columns;
            list.data = [].concat( search( cat.list ) );
            _.call( options.ondone, null, list );
         } else {
            _.info( 'Searching all categories: ' + options.term  );
            list.columns = ["ID","Name","Category","Type","Level","SourceBook"];
            list.data = [];
            od.data.get().forEach( function( c ) {
               list.data = list.data.concat( search( c.list ) );
            } );
            _.call( options.ondone, null, list );
         }

         function search( lst ) {
            var result = [];
search_loop:
            for ( var i = 0, l = lst.length ; i < l ; i++ ) {
               var row = lst[i];
               for ( var prop in row ) {
                  if ( prop !== '_category' ) {
                     var p = row[prop];
                     if ( regx.test( p.text ? p.text : p ) ) {
                        result.push( row );
                        continue search_loop;
                     }
                  }
               }
               if ( search_body && regx.test( row._category.index[row.ID] ) ) {
                  result.push( row );
               }
            }
            return result;
         }
      }
   },

   "list_category" : function data_search_list_category ( cat, onload ) {
      od.data.load_catalog( function data_search_list_category_load() {
         var list = { "columns":[], "data":[] };
         if ( cat ) {
            _.info( 'List ' + cat.title );
            cat.load_extended( function data_search_list_category_load_cat() {
               list.columns = cat.columns;
               list.data = [].concat( cat.list );
               _.call( onload, cat, list );
            });
         } else {
            _.info( 'List all categories' );
            list.columns = ["ID","Name","Category","Type","Level","SourceBook"];
            od.data.load_all_listing( function data_search_list_category_load_all(){
               od.data.get().forEach( function data_search_list_category_each( c ) {
                  list.data = list.data.concat( c.list );
               } );
               _.call( onload, null, list );
            });
         }
      } );
   },

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Given a textual search term, return a regular expression and highlight terms.
    *
    * Syntax: Terms are separated by space, can be double quoted, may appears in any order, case in-sensitive.
    *         Terms with leading '-' are excluded from result. Terms starting and ending with / are treated as regular expression.
    *         The word "OR" (uppercase, without quote) can be used to specify an or condition of two terms.
    *         Terms are not searched word by word, so for example they can appears in the middle of a word.
    *
    * Example : javascript OR ecmascript "bug database"
    *
    * @param {String} terms  Terms to search for
    * @return {Array} [ RegExp, ["highlight 1", "highlight 2", ... ] ] OR null (if terms turn out to be empty conditions)
    */
   "gen_search" : function search_gen_search ( terms ) {
      var hl = [];
      var regx = "^";
      // Break down search input into tokens
      var parts = terms.match( /[+-]?(?:"[^"]+"|\S+)/g );
      if ( ! parts ) return null;

      // Remove leading / trailing OR which is invalid
      while ( parts.length > 0 && parts[0] === 'OR' ) parts.splice(0,1);
      var l = parts.length;
      while ( l > 0 && parts[l-1] === 'OR' ) parts.splice(--l,1);

      for ( var i = 0 ; i < l ; ) {
         // Contains all parts joined by OR, e.g. a OR b OR c >>> ['(?=.*a.*)','(?=.*b.*)','(?=.*c.*)']
         var addPart = [];
         do {
            var term = parts[i];
            var part = "";
            // Detect whether to include or exclude term
            if ( term.indexOf('-') === 0 ) {
               term = term.substr(1);
               part += '(?!.*';
            } else {
               if ( term.indexOf('+') === 0 ) term = term.substr(1);
               part += '(?=.*';
            }
            if ( term ) {
               // Regular expression is used as is.
               if ( /^\/.+\/$/.test( term ) ) {
                  part += term.substr( 1, term.length-2 );
                  term = ''; // prevent term highlight

               // Terms need to be unquoted first
               } else if ( /^"[^"]+"$/.test( term ) ) {
                  term = term.substr( 1, term.length-2 );
                  part += _.escRegx( term );

               // Otherwise is normal word, just need to unescape
               } else {
                  part += _.escRegx(term);
               }

               // If not exclude, add term to highlight
               if ( part ) {
                  if ( part.indexOf( '(?=' ) === 0 && term ) hl.push( term );
                  part += '.*)';
                  addPart.push( part );
               }
            }
            // Advance pointer and check whether next token is not OR
            i++;
            if ( i >= l || parts[i] !== 'OR' ) break;
            // Next token is OR, so move to next non-OR token.
            do { ++i; } while ( i < l && parts[i] === 'OR' );
         } while ( i < l );
         // Append to global search pattern
         if ( addPart.length === 1 ) {
            regx += addPart[0];
         } else if ( addPart.length ) {
            regx += '(?:' + addPart.join('|') + ')';
         }
      }
      if ( regx === '^' ) return null;
      _.debug( [ regx, hl ] );
      return [ new RegExp( regx, 'i' ), hl ];
   }
};