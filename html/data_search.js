/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * data_search.js
 *
 * Handle search logic, including parsing search pattern, marking highlight terms,
 * and actually searching the data model.
 *
 * Can also list unfiltered entry list in same format as search result.
 */

od.search = {
   /** Search cache */
   cache : {
      type : "", // name or full
      category : {}, // Result map by category
      count: {}, // Count for other category is not overritten until term changes.
      term : 'global search term' // By-field search happens after global.
   },

   /**
    * Call to do a search.  Will do a search after required lising and indices are loaded.
    * @param {Object} options Options: {
    *    category : category object, empty for all categories.
    *    term : terms to search for.
    *    type : if 'full', search both listing and content, otherwise search listing only.
    *    ondone : callback after search is finished with search result { columns, list }, which may be zero rows.
    * }
    */
   "search" : function search_search ( options ) {
      _.time();
      var cache = od.search.cache, cat = options.category, cols;
      var term = options.term, type = options.type;
      if ( term !== cache.term || cache.type !== type ) {
         cache = od.search.cache = {
            category : {},
            count : {},
            term : term,
            type : type
         };
      }
      var pattern = options.term ? this.gen_search( options.term ) : null;
      if ( cat ) {
         // Search in a single category
         cat.load_listing( function search_search_cat () {
            cols = od.config.display_columns( cat.columns );
            if ( ! pattern ) return _.call( options.ondone, null, cols );
            if ( type === 'full' ) {
               cat.load_index( do_search );
            } else {
               do_search();
            }
         });
      } else {
         // Search in all categories
         cols = ["ID","Name","Category","Type","Level","SourceBook"];
         if ( ! pattern ) return _.call( options.ondone, null, cols );
         od.data.load_all_listing( function search_search_all () {
            if ( type === 'full' ) {
               od.data.load_all_index( do_search );
            } else {
               do_search();
            }
         });
      }

      function done ( result, count ) {
         _.call( options.ondone, null, cols, result, count, pattern.highlight );
      }

      /** Called after all data needed for search is properly loaded. */
      function do_search () {
         var count = cache.count, result;
         if ( cat ) {
            result = cache[ cat.name ];
            if ( ! result ) {
               _.time( 'Search ' + cat.name + ': ' + options.term );
               cache[ cat.name ] = result = search( cat.list );
               count[ cat.name ] = result.length;
               _.time( 'Search done, ' + result.length + ' result(s).' );
            }
         } else {
            result = cache[ '' ];
            if ( ! result ) {
               _.time( 'Searching all categories: ' + options.term  );
               result = [];
               count[''] = 0;
               od.data.get().forEach( function search_search_each ( cat ) {
                  var data = cache[ cat.name ];
                  if ( ! data ) {
                     cache[ cat.name ] = data = search( cat.list );
                     count[ cat.name ] = data.length;
                  }
                  result = result.concat( data );
                  count[ '' ] += data.length;
               } );
               cache[ '' ] = result;
               _.time( 'Search done, ' + result.length + ' result(s).' );
            }
         }
         done( result, count );

         function search( lst ) {
            var regx = pattern.regexp;
            var result = lst.filter( function search_search_filter ( row ) {
               if ( type !== 'full' ) {
                  // Name search. Just try to match name.
                  return regx.test( row.Name );
               } else {
                  // Full body search.  If does not have exclude term, try name first. If fail or otherwise do full body.
                  if ( ( ! pattern.hasExclude ) && regx.test( row.Name ) ) return true;
                  return regx.test( row._category.index[ row.ID ] );
               }
            } );
            return result;
         }
      }
   },

   "list_category" : function data_search_list_category ( cat, onload ) {
      _.time();
      var list = { "columns":[], "data":[] };
      if ( cat ) {
         _.time( 'List ' + cat.name );
         cat.load_listing( function data_search_list_category_load_cat() {
            _.call( onload, null, od.config.display_columns( cat.columns ), cat.list.concat(), null, null );
         } );
      } else {
         _.time( 'List all categories' );
         od.data.load_all_listing( function data_search_list_category_load_all(){
            var data = [];
            od.data.get().forEach( function data_search_list_category_each( c ) {
               data = data.concat( c.list );
            } );
            _.call( onload, null, ["ID","Name","Category","Type","Level","SourceBook"], data, null, null );
         } );
      }
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
    * @return {Array} return { 'regexp': RegExp for searching, 'highlight': ["highlight 1", "highlight 2", ... ], 'hasExclude': true/false }
    *                        OR null (if terms turn out to be empty conditions)
    */
   "gen_search" : function search_gen_search ( terms ) {
      var hl = [], hasExclude = false;
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
            var is_whole_word = false;
            // Detect whether to include or exclude term
            if ( term.charAt(0) === '-' ) {
               term = term.substr(1);
               part += '(?!.*'; // Exclude
               hasExclude = true;
            } else {
               if ( term.charAt(0) === '+' ) {
                  term = term.substr(1);
                  is_whole_word = true;
               }
               part += '(?=.*'; // Include
            }
            if ( term ) {
               // Regular expression is used as is.
               if ( /^\/.+\/$/.test( term ) ) {
                  term = term.substr( 1, term.length-2 );

               // Quoted terms need to be unquoted first
               } else if ( /^"[^"]*"$/.test( term ) ) {
                  term = term.length > 2 ? _.escRegx( term.substr( 1, term.length-2 ) ) : '';

               // Otherwise is normal word, just need to unescape
               } else {
                  // Remove leading double quote for incomplete terms
                  if ( term.charAt(0) === '"' ) term = term.substr( 1 );
                  if ( term ) term = _.escRegx( term );
               }
               if ( term ) {
                  if ( is_whole_word ) term = '\\b' + term + '\\b';
                  part += term;
               }

               // If not exclude, add term to highlight
               if ( part ) {
                  if ( part.indexOf( '(?=' ) === 0 && term && term.length > 2 )
                     hl.push( term );
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
      return { 'regexp': RegExp( regx, 'i' ), 'highlight': hl.length ? hl : null, 'hasExclude': hasExclude };
   }

};