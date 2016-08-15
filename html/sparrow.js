/**
 *
 * sparrow.js
 *
 * Sparrow - light weight JS library. Lower level and boarder then JQuery, not DOM oriented.
 *
 * Feature support varies by browser, target is latest IE, Chrome, Firefox
 *
 */
(function sparrrow_init() { 'use strict';

var ns = this;

// Simple check for browser features
//if ( ! document.querySelectorAll || !window.Storage ) {
//   alert('Please upgrade browser or switch to a new one.');
//}

/**
 * Select DOM Nodes by CSS selector.
 *
 * @expose
 * @param {(string|Node)} root Optional. Root node to select from. Default to document.
 * @param {string=} selector CSS selector to run. Has shortcut for simple id/class/tag.
 * @returns {Array|NodeList} Array or NodeList of DOM Node result.
 */
var _ = function sparrow ( root, selector ) {
   if ( selector === undefined ) {
      selector = root;
      root = document;
   }
   if ( ! selector ) return [ root ];
   // Test for simple id / class / tag, if fail then use querySelectorAll
   if ( selector.indexOf(' ') > 0 || ! /^[#.]?\w+$/.test( selector ) ) return root.querySelectorAll( selector );

   // Get Element series is many times faster then querySelectorAll
   if ( selector.charAt(0) === '#' ) {
      var result = root.getElementById( selector.substr(1) );
      return result ? [ result ] : [ ];
   }
   if ( selector.charAt(0) === '.' ) return root.getElementsByClassName( selector.substr(1) );
   return root.getElementsByTagName( selector );
};

if ( ns ) ns._ = _;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// General Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Safely get the (deep) property of a object.
 * If the property is unavailable, return undefined.
 * e.g. _.get( window, [ 'localStorage', 'sessionStorage' ], 'datakey' );
 *
 * @param {*} root Root object to start access
 * @returns {*} Specified property, or undefined
 */
_.get = function _get ( root /*, property */ ) {
   return _.getd.apply( this, _.ary( arguments ).concat( undefined ) );
};

/**
 * Safely get the (deep) property of a object.
 * If the property is unavailable, return last parameter.
 * e.g. _.get( window, [ 'localStorage', 'sessionStorage' ], 'datakey', 'default setting' );
 *
 * @param {*} root Root object to start access
 * @param {*} defVal (Last parameter) Default value if property is not found.
 * @returns {*} Specified property, or defVal
 */
_.getd = function _getd ( root /*, property, defVal */ ) {
   var base = root, len = arguments.length-1, defVal= arguments[ len ];
   for ( var i = 1 ; i < len ; i++ ) {
      if ( base === null || base === undefined ) return base;
      base = Object( base );
      var prop = arguments[ i ];
      if ( Array.isArray( prop ) ) { // Try each candidate and continue with first match
         for ( var j in prop ) { var p = prop[ j ];
            if ( p in base ) {
               base = base[ p ];
               j = null;
               break;
            }
         }
         if ( j !== null ) return defVal;
      } else {
         if ( prop in base ) base = base[ prop ];
         else return defVal;
      }
   }
   return base;
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Array Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert an array-like object to be an array.
 *
 * @param {(Array|NodeList|*)} subject Subject to be converted.
 * @param {integer=} startpos If given, work like Array.slice( startpos ).
 * @param {integer=} length If this and startpos is given, work like Array.slice( startpos, length ).
 * @returns {(Array|null|undefined)} Clone or slice of subject.  Or subject, if subject is null or undefined.
 */
_.ary = function _ary ( subject, startpos, length ) {
   if ( ! Array.isArray( subject ) ) {
      if ( subject === null || subject === undefined ) return subject;
      if ( typeof( subject ) === 'string' || typeof( subject ) === 'function' || typeof( subject.length ) !== 'number' ) return [ subject ];
      if ( subject.length <= 0 ) return [];
      if ( startpos || length )
         return Array.prototype.slice.call( subject, startpos, length );
      else
         return Array.from ? Array.from( subject ) : Array.prototype.slice.call( subject );
   }
   return startpos || length ? subject : subject.slice( startpos, length );
};

/**
 * Convert an array-like object to be an array.  Will always return an array.
 *
 * @param {(Array|NodeList|*)} subject Subject to be converted.
 * @param {integer=} startpos If given, work like Array.slice( startpos ).
 * @param {integer=} length If this and startpos is given, work like Array.slice( startpos, length ).
 * @returns {Array} Clone or slice of subject.
 */
_.array = function _array ( subject, startpos, length ) {
   if ( subject === null || subject === undefined ) return [];
   return _.ary( subject, startpos, length );
};

/**
 * Call forEach on an array-like object.
 * @param {(Array|NodeList|*)} subject Subject to call forEach on
 * @param {Function=} callbcak Callback function (element, index)
 * @param {*=} thisarg The 'this' argument of callback.  Passed straight to Array.forEach.
 */
_.forEach = function _forEach ( subject, callback, thisarg ) {
   if ( subject === null || subject === undefined || callback === null || callback === undefined ) return;
   if ( subject.forEach ) return subject.forEach( callback, thisarg );
   return Array.prototype.forEach.call( subject, callback, thisarg );
};

/**
 * alias of _.map
 *
 * @param {(Array|NodeList|Object)} subject Array-like object to be extracted.
 * @param {string=} column Columns (field) to extract.
 * @returns {Array} Array (if single column) or Array of Object (if multiple columns).
 */
_.col = function _col ( subject, column /* ... */) {
   return _.map( _.ary( subject ), column === undefined ? 0 : column );
};

/**
 * Returns a sorter function that sort an array of items by given fields.
 *
 * @param {string} field Field name to compare.
 * @param {boolean=} des true for a descending sorter. false for ascending sorter (default).
 * @returns {function(*,*)} Sorter function
 */
_.sorter = function _sorter ( field, des ) {
   var ab = ! des ? 1 : -1, ba = -ab;
   if ( field === undefined || field === null ) return function _sorter_val( a, b ) { return a > b ? ab : ( a < b ? ba : 0 ); };
   return function _sorter_fld( a, b ) { return a[ field ] > b[ field ] ? ab : ( a[ field ] < b[ field ] ? ba : 0 ); };
};

/**
 * Returns a sorter function that sort an array of items by given fields.
 *
 * @param {string} field Field name to compare, leave undefined to compare the value itself.
 * @param {boolean=} des true for a descending sorter. false for ascending sorter (default).
 * @returns {function(*,*)} Sorter function
 */
_.sorter.number = function _sorter_number ( field, des ) {
   var ab = ! des ? 1 : -1, ba = -ab;
   if ( field === undefined || field === null ) {
      return function _sorter_number_val( a, b ) { return +a > +b ? ab : ( +a < +b ? ba : 0 ); };
   } else {
      return function _sorter_number_fld( a, b ) { return +a[ field ] > +b[ field ] ? ab : ( +a[ field ] < +b[ field ] ? ba : 0 ); };
   }
};

/**
 * Sort given array-like data by given fields.
 *
 * @param {Array} data Data to sort. Will be modified and returned.
 * @param {string} field Field name to compare
 * @param {boolean=} des   true for a descending sort. false for ascending sort (default).
 * @returns {Array} Sorted data.
 */
_.sort = function _sort ( data, field, des ) {
   return _.ary( data ).sort( _.sorter( field, des ) );
};

/**
 * Returns a mapper function that returns a specefic field(s) of input data.
 *
 * @param {string|Array} field Name of field to grab.  If array, will grab the properties in hierical order, stopping at null and undefined but not at numbers.
 * @returns {function} Function to apply mapping.
 */
_.mapper = function _mapper ( field ) {
   var arg = arguments, len = arg.length;
   if ( len <= 0 ) return _.dummy();
   if ( len === 1 && typeof( field ) === 'string' ) {
      return function _mapper_prop ( v ) { return v[ field ]; };
   }
   return function _mapper_dynamic ( v ) {
      var result = [], map_func = _.mapper._map;
      for ( var i = 0 ; i < len ; i++ ) {
         result.push( map_func( v, arg[ i ] ) );
      }
      return len === 1 ? result[0] : result;
   };
};
/** Mapper function for internal use. */
_.mapper._map = function _mapper_map( base, prop ) {
   if ( _.is.literal( prop ) ) {
      // String
      return base[ prop ];

   } else if ( Array.isArray( prop ) ) {
      // Array
      for ( var i = 0, len = prop.length ; i < len ; i++ ) {
         if ( base === undefined || base === null ) return base;
         base = base[ prop[ i ] ];
      }
      return base;

   } else {
      // Object, assume to be property map.
      var result = _.map();
      for ( var p in prop ) {
         result[ p ] = _mapper_map( base, prop[ p ] );
      }
      return result;
   }
};

/**
 * 1) (No parametres) Return an empty object (null prototype) to be used
 * 2) (With params) Map given array-like data, by field name or by callback.
 *
 * @param {Array} data Data to map. Will be modified and returned.
 * @param {string|Array} field Name of field to grab or mapping to perform.
 * @returns {Array} Mapped data.
 */
_.map = function _map ( data, field ) {
   if ( arguments.length === 0 ) return Object.create( null );
   if ( typeof( field ) === 'function' )
      return Array.prototype.map.call( data, field );
   else
      return Array.prototype.map.apply( data, _.mapper.apply( null, _.ary( arguments, 1 ) ) );
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Text Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert the first letter of input to upper case, and return whole string.
 *
 * @param {String} txt String to convert. Will be casted to a string.
 * @returns {String} Converted string.
 */
_.ucfirst = function _ucfirst ( txt ) {
   return txt ? String(txt).substr(0,1).toUpperCase() + txt.substr(1) : txt;
};

/**
 * Convert the first letter of each word, and return whole string.
 * Word is detected by word boundary, as defined by regular expression.
 *
 * @param {String} txt String to convert. Will be casted to a string.
 * @returns {String} Converted string.
 */
_.ucword = function _ucword ( txt ) {
   return txt ? String(txt).split( /\b(?=[a-zA-Z])/g ).map( _.ucfirst ).join( '' ) : txt;
};

/**
 * Get data from localStorage.
 * If a key is not found, result will be undefined.
 * e.g. _.pref({ id: "myapp.lastid" }, { id: "defaultId" }) => { id: "localStorage['myapp.lastid'] or defaultId" }
 *
 * @param {(String|Array|Object)=} key Key to get, Map of 'return':'key', or array of key/map to get.
 * @param {*=} defaultValue If key is string, the default value to return.  Othewise this value will be merged into result using _.extend.
 */
_.pref = function _pref ( key, defaultValue ) {
   if ( arguments.length <= 1 ) defaultValue = null;
   if ( window.localStorage ) {
      var store = localStorage;
      if ( key === undefined ) {
         return Array( store.length ).fill( undefined ).map( function _pref_list ( e, i ) {
            return store.key( i );
         } );
      }
      if ( Array.isArray( key ) ) {
         return _.extend( key.map( function _pref_each ( k ) {
            return _pref( k );
         } ), defaultValue );
      }
      if ( _.is.object( key ) ) {
         var result = {};
         for ( var k in key ) {
            var v  = getter( key[ k ] );
            if ( v !== undefined ) result[ k ] = v;
         }
         return _.extend( result, defaultValue );
      }
      return getter( key, defaultValue );
   } else {
      return key === undefined ? [] : defaultValue;
   }

   function getter ( k, def ) {
      var val = store.getItem( k );
      if ( val && val.match( /^\t\n\r(?=\S).*(?:\S)\n\r\t$/ ) ) try {
         val = JSON.parse( val );
      } catch ( err ) {}
      else if ( val === null ) return def;
      return val;
   }
};

/**
 * Set data to localStorage.
 * Simple values and objects, such as null or { a: 1 }, will auto stringify to JSON on set and parsed on get.
 * e.g. _.pref.set({ "conf":{"hide":1} }) => localStorage.setItem( 'conf', ' {"hide":1} ' );
 *
 * (Because of JSON, NaN will be converted to null, whether plain value or part of object value.)
 *
 * @param {(String|Array|Object)=} key Key to get, Map of 'key':'value', or array of key/map to get.
 * @param {*=} value Value to set.  If both key and value is an array, will try to map values to the key.
 *                   Otherwise will be stored as string or as json string.
 */
_.pref.set = function _pref_set ( key, value ) {
   if ( window.localStorage ) {
      var store = window.localStorage;
      if ( _.is.literal( key ) ) {
         setter( key, value );
      } else if ( Array.isArray( key ) ) {
         key.forEach( function( e, i ) { _pref_set( e, _.getd( value, i, value ) ); } );
      } else if ( _.is.object( key ) ) {
         for ( var k in key ) setter( k, key[ k ] );
      } else {
         throw '[sparrow.pref.set] Unknown key, must be string, array, or object.';
      }
   }
   function setter ( k, value ) {
      if ( value === undefined ) {
         store.removeItem( k );
      } else {
         _.assert( _.is.literal( value ) || value.__proto__ === null || value.__proto__ === Object.prototype || value instanceof Array,
            "Preference value must be literal or simple object." );
         if ( typeof( value ) !== 'string' && ( _.is.literal( value ) || _.is.object( value ) ) ) {
            value = "\t\n\r" + JSON.stringify( value ) + "\n\r\t"; // JSON only has four space character. Not much choices here.
         }
         store.setItem( k, value );
      }
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Function Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * A function that literally do nothing.  This function can be shared by code that need such a dummy function.
 */
_.dummy = function _dummy () {};

/**
 * A function that returns whatever passed in.
 * @param  {*} v Any input.
 * @return {*} Output first parameter.
 */
_.echo = function _echo ( v ) { return v; };

/**
 * Call a function - but only if it is defined - and return its return value.
 *
 * @param {Function} func   function to call. Must be function, null, or undefined.
 * @param {Object} thisObj  'this' to be passed to the function
 * @param {...*} param      call parameters, can have multiple.
 * @returns {*}             Return value of called function, or undefined if function is not called or has error.
 */
_.call = function _call ( func, thisObj, param /*...*/ ) {
   if ( func === undefined || func === null ) return undefined;
   return func.apply( thisObj, _.ary(arguments, 2) );
};

/**
 * Given a function, return a function that when called multiple times, only the first call will be executed.
 *
 * Parameters passed to the returned function will be supplied to the callback as is.
 * This function will disregard any additional parameters.
 *
 * @param {Function} func  Function to call.
 * @returns {Function} Function that can be safely called multiple times without calling func more then once
 */
_.callonce = function _call ( func ) {
   if ( ! func ) return _.dummy;
   return function _callonce_call () {
      if ( ! func ) return; // func would be set to null after first call
      var f = func;
      func = null;
      return f.apply( this, arguments );
   };
};

/*
 * Call a function immediately after current JS stack is resolved.
 *
 * @param {function(...*)} func Function to call
 * @returns {integer} Id of callback.
 */
if ( this && this.setImmediate ) {
   _.setImmediate = this.setImmediate.bind( this );
   _.clearImmediate = this.clearImmediate.bind( this );

} else if ( this && ns.requestAnimationFrame ) {
   _.setImmediate = this.requestAnimationFrame.bind( this );
   _.clearImmediate = this.cancelAnimationFrame.bind( this );

} else {
   _.setImmediate = function setImmediate ( func ) { return setTimeout( func, 0 ); };
   _.clearImmediate = this.clearTimeout;
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Ajax / js / Cross origin inclusion
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Load a JavaScript from an url.
 *
 * Options:
 *   url - Url to send get request, or an option object.
 *   charset - Charset to use.
 *   type    - script type.
 *   validate - Callback; if it returns true, script will not be loaded,
 *                        otherwise if still non-true after load then call onerror.
 *   onload  - Callback (url, option) when the request succeed.
 *   onerror - Callback (url, option) when the request failed.
 *
 * @param {(string|Object)} option Url to send get request, or an option object.
 * @param {function(string,Object)=} onload Overrides option.onload
 * @returns {Element|undefined} Created script tag.
 */
_.js = function _js ( option, onload ) {
   if ( typeof( option ) === 'string' ) option = { url: option };
   if ( onload !== undefined ) option.onload = onload;

   // Validate before doing anything, if pass then we are done
   if ( option.validate && option.validate.call( null, url, option ) ) return _js_done( option.onload );

   var url = option.url;

   var attr = { 'src' : url, 'parent': document.body || document.head };
   if ( option.charset ) attr.charset = option.charset;
   if ( option.type ) attr.type = option.type;
   if ( option.async ) attr.async = option.async;

   var e = _.create( 'script', attr );
   _.info( "[JS] Load script: " + url );

   var done = false;
   function _js_done ( callback, log ) {
      if ( done ) return;
      done = true;
      if ( log ) _.info( log );
      _.call( callback, e, url, option );
      _.call( option.ondone || null, e, url, option );
      if ( e && e.parentNode === document.body ) document.body.removeChild(e);
   }

   e.addEventListener( 'load', function _js_load () {
      // Delay execution to make sure validate/load is called _after_ script has been ran.
      _.setImmediate( function _js_load_delayed () {
         if ( option.validate && ! _.call( option.validate, e, url, option )  ) {
            return _js_done( option.onerror, "[JS] Script loaded but fails validation: " + url );
         }
         _js_done( option.onload, "[JS] Script loaded: " + url );
      } );
   } );
   e.addEventListener( 'error', function _js_error ( e ) {
      _js_done( option.onerror, "[JS] Script error or not found: " + url );
   } );
   return e;
};

_.is = {
   /**
    * Detect whether browser ie IE.
    * @returns {boolean} True if browser is Internet Explorer, false otherwise.
    */
   ie : function _is_ie () {
      var result = /\bMSIE \d|\bTrident\/\d\b./.test( navigator.userAgent );
      _.is.ie = function () { return result; };
      return result;
   },

   /**
    * Detect whether browser ie Firefox.
    * @returns {boolean} True if browser is Firefox, false otherwise.
    */
   firefox : function _is_firefox () {
      var result = /\bGecko\/\d{8}/.test( navigator.userAgent );
      _.is.firefox = function () { return result; };
      return result;
   },

   /**
    * Retuan true if given value is a literal value (instead of an object)
    * @param {*} val Value to check.
    * @returns {(undefined|null|boolean)} True if value is boolean, number, or string. Undefined or null if input is one of them.  False otherwise.
    */
   literal : function _is_literal ( val ) {
      if ( val === undefined || val === null ) return val;
      var type = typeof( val );
      return type === 'boolean' || type === 'number' || type === 'string';
   },

   /**
    * Retuan true if given value is an object (instead of literals)
    * @param {*} val Value to check.
    * @returns {(undefined|null|boolean)} True if value is object or function. Undefined or null if input is one of them.  False otherwise.
    */
   object : function _is_object ( val ) {
      if ( val === undefined || val === null ) return val;
      var type = typeof( val );
      return type === 'object' || type === 'function';
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Document parsing
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * A) Parse html and return a dom element or a document fragment.
 * B) Set a dom element's html.
 *
 * @param {(string|DOMElement|NodeList)} txt HTML text to parse.
 * @param {string=} html HTML text to parse.
 * @returns {Node} A div element that contains parsed html as dom child.
 */
_.html = function _html ( txt, html ) {
   if ( html === undefined && typeof( txt ) === 'string' ) {
      var frag, range = _html.range || ( _html.range = document.createRange() );
      try {
         frag = range.createContextualFragment( txt );
      } catch ( err ) {
         frag = range.createContextualFragment( '<body>' + txt + '</body>' );
      }
      return frag.childElementCount > 1 ? frag : frag.firstElementChild;
   } else {
      _.forEach( _.domList( txt ), function _html_each( e ) {
         e.innerHTML = html;
      });
   }
};

_.html.contains = function _html_contains( root, child ) {
   return root === child || ( root.compareDocumentPosition( child ) & 16 );
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Console logging & timing.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Console log function.
 * Will automaticall switch to dir or table when subject is object or array.
 * This behaviour does not apply to info/warn/error
 *
 * @param {string} type Optional. Type of console function to run, e.g. 'info' or 'warn'. If not found then fallback to 'log'.
 * @param {*=}  msg  Message objects to pass to console function.
 */
_.log = function _log ( type, msg ) {
   if ( msg === undefined ) {
      msg = type;
      type = 'log';
   }
   if ( ns.console ) {
      if ( ! ns.console[type] ) type = 'log';
      if ( type === 'log' ) {
         if ( ns.console.table && Array.isArray( msg ) ) type = 'table';
         else if ( ns.console.dir && _.is.object( msg ) ) type = 'dir';
      };
      ns.console[type]( msg );
   }
};

/**
 * Safe console.info message.
 *
 * @param {*} msg Message objects to pass to console.
 */
_.info = function _info ( msg ) { _.log( 'info', msg ); };

/**
 * Safe console.warn message.
 *
 * @param {*} msg Message objects to pass to console.
 */
_.warn = function _warn ( msg ) { _.log( 'warn', msg ); };

/**
 * Safe console.warn message.
 *
 * @param {*} msg Message objects to pass to console.
 */
_.error = function _error ( msg ) { _.log( 'error', msg ); };

/**
 * An alert function that will stack up all errors in a 50ms window and shows them together.
 * Duplicate messages in same window will be ignored.
 *
 * @param {*} msg Message objects to pass to console.
 */
_.alert = function _alert ( msg ) {
   if ( ! _.alert.timeout ) {
      // Delay a small period so that errors popup together instead of one by one
      _.alert.timeout = setTimeout( function _error_timeout(){
         _.alert.timeout = 0;
         alert( _.alert.log );
         _.alert.log = [];
      }, 50 );
   }
   if ( _.alert.log.indexOf( msg ) < 0 ) {
      _.alert.log.push( msg );
   }
};
_.alert.timeout = 0;
_.alert.log = [];

/**
 * Coarse timing function. Will show time relative to previous call as well as last reset call.
 * Time is in unit of ms. This routine is not designed for fine-grain measurement that would justify using high performance timer.
 *
 * @param {string=} msg Message to display.  If undefined then will reset accumulated time.
 * @returns {(Array|undefined)} Return [time from last call, accumulated time].
 */
_.time = function _time ( msg ) {
   var t = _.time;
   var now = ns.performance ? performance.now() : Date.now();
   if ( msg === undefined ) {
      t.base = now;
      t.last = null;
      return now;
   }
   var fromBase = Math.round( ( now - t.base ) * 1000 ) / 1000;
   var fromLast = Math.round( ( now - t.last ) * 1000 ) / 1000;
   var txtLast = t.last ? ( 'ms,+' + fromLast ) : '';
   _.info( msg + ' (+' + fromBase + txtLast + 'ms)' );
   t.last = now;
   return [fromLast, fromBase];
};

if ( ns.console && ns.console.assert ) {
   _.assert = ns.console.assert.bind( ns.console );
} else {
   _.assert = _.dummy();
}

_.log.group = function _log_group( msg ) {
   if ( ns.console && ns.console.group ) return ns.console.group( msg );
   return _.log( msg );
};

_.log.collapse = function _log_groupCollapsed( msg ) {
   if ( ns.console && ns.console.groupCollapsed ) return ns.console.groupCollapsed( msg );
   return _.log( msg );
};

_.log.end = function _log_groupEnd() {
   if ( ns.console && ns.console.groupEnd ) return ns.console.groupEnd();
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// String helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * HTML escape function.
 *
 * @param {string} txt  Text to do HTML escape.
 * @returns {string} Escaped text.
 */
_.escHtml = function _escHtml ( txt ) {
   if ( ! /[<&'"]/.test( txt ) ) return txt;
   //return t.replace( /&/g, '&amp;').replace( /</g, '&lt;').replace( /"/g, '&quot;' ).replace( /'/g, '&#39;' );
   return txt.replace( /[&<"']/g, function ( c ) { return { '&':'&amp;', '<':'&lt;', '"':"&quot;", "'":'&#39;' }[ c ]; });
};

/**
 * JavaScript escape function.
 *
 * @param {string} txt  Text to do JavaScript escape.
 * @returns {string} Escaped text.
 */
_.escJs = function _escJs ( txt ) {
   return txt.replace( /\r?\n/g, '\\n').replace( /'"/g, '\\$0');
};

/**
 * Regular expressoin escape function.
 *
 * @param {string} txt  Text to do regx escape.
 * @returns {string} Escaped text.
 */
_.escRegx = function _escRegx ( txt ) {
   return txt.replace( /([()?*+.\\{}[\]])/g, '\\$1' );
};

/**
 * UTF-8 safe base64 encode function
 * @param {String} data Data string
 * @returns {String} Base64 encoded URI escaped data
 */
_.btoa = function _btoa ( data ) { return btoa( encodeURIComponent( data ) ); };

/**
 * UTF-8 safe base64 decode function
 * @param {String} data Base64 encoded URI escaped data
 * @returns {String} Original data
 */
_.atob = function _atob ( data ) { return decodeURIComponent( atob( data ) ); };

/**
 * Round function with decimal control.
 *
 * @param {number} val Number to round.
 * @param {integer=} decimal Optional. Decimal point to round to. Negative will round before decimal point. Default to 0.
 * @returns {integer} Rounded number.
 */
_.round = function _round ( val, decimal ) {
   var e = Math.pow( 10, ~~decimal );
   //if ( e === 1 ) return Math.round( val );
   return Math.round( val *= e ) / e;
};

/**
 * Convert big number to si unit or vice versa. Case insensitive.
 * Support k (kilo, 1e3), M, G, T, P (peta, 1e15)
 *
 * @param {(string|number)} val  Number to convert to unit (e.g. 2.3e10) or united text to convert to number (e.g."23k").
 * @param {integer=} decimal Optional. Decimal point of converted unit (if number to text). See _.round.
 * @returns {(string|number)} Converted text or number
 */
_.si = function _si ( val, decimal ) {
   if ( typeof( val ) === 'string' ) {
      if ( ! /^-?\d+(\.\d+)?[kmgtp]$/i.test( val ) ) return +val;
      var l = val.length-1, c = val.charAt( l ).toLowerCase();
      return val.substr( 0, l )*{'k':1e3,'m':1e6,'g':1e9,'t':1e12,'p':1e15}[c];
   } else {
      var count = 0;
      while ( val > 1000 || val < -1000 ) { val /= 1000; ++count; }
      return _.round( val, decimal ) + ['','k','M','G','T','P'][count];
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Object helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Add properties from one object to another.
 * Properties owned by target will not be overwritten, but inherited properties may be copied over.
 * Similiarly, properties owned by subsequence arguments will be copied, but not inherited properties.
 *
 * @param {Object} target Target object, will have its properties expanded.
 * @param {Object} copyFrom An object with properties
 * @returns {Object} Extended target object
 */
_.extend = function _extend( target, copyFrom ) {
   var prop = [], exists = Object.getOwnPropertyNames( target );
   if ( Object.getOwnPropertySymbols ) exists = exists.concat( Object.getOwnPropertySymbols( target ) );
   for ( var i = 1, len = arguments.length ; i < len ; i++ ) {
      var from = arguments[ i ];
      if ( from === undefined || from === null ) continue;
      var keys = Object.getOwnPropertyNames( from );
      if ( Object.getOwnPropertySymbols ) keys = keys.concat( Object.getOwnPropertySymbols( from ) );
      for ( var i in keys ) {
         var name = keys[ i ];
         if ( exists.indexOf( name ) < 0 ) {
            prop[ name ] = Object.getOwnPropertyDescriptor( from, name );
            exists.push( name );
         }
      }
   }
   if ( prop.length )
      Object.defineProperties( target, prop );
   return target;
};

/**
 * Set or define a list of object's property.
 *
 * @param {(string|Node|NodeList|Array|Object)} ary Element selcetor, dom list, or Array of JS objects.
 * @param {(Object|string)} obj Attribute or attribute object to set.
 * @param {*=} value Value to set.
 * @param {string=} flag w for non-writable, e for non-enumerable, c for non-configurable.  Can start with '^' for easier understanding.
 * @returns {Array} Array-ifyed ary.
 */
_.prop = function _prop ( ary, obj, flag ) {
   ary = _.domList( ary );
   _.assert( _.is.object( obj ), '[sparrow.prop] Set target must be a map object.' );
   _.assert( ! ( flag && _.get( ary, 0 ) instanceof Node ), '[sparrow.prop] Property flags cannot be set on DOM elements' );
   // If no flag, we will use the simple method.  Notice that we will not delete properties, use _.curtail for that.
   if ( ! flag || flag === '^' || ! Object.defineProperties ) {
      var setter = function _prop_set ( e ) {
         for ( var name in obj ) {
            e[ name ] = obj[ name ];
         }
      };
   } else {
      // Need to set property properties.
      flag = flag.toLowerCase();
      var props = {}
        , c = flag.indexOf( 'c' ) < 0  // False if have 'c'
        , e = flag.indexOf( 'e' ) < 0  // False if have 'e'
        , w = flag.indexOf( 'w' ) < 0; // False if have 'w'
      for ( var name in obj ) {
         props[ name ] = {
           value : obj[ name ],
           configurable : c,
           enumerable : e,
           writable : w
         };
      }
      var setter = function _prop_define ( e ) {
         Object.defineProperties( e, prop );
      };
   }
   _.forEach( ary, setter );
   return ary;
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// DOM operation
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Call 'preventDefault' (if exist) and return false.
 * @param {Event} e Event object
 * @returns {Boolean} always return false; return this to make sure event is stopped.
 */
_.noDef = function _noDef ( e ) { if ( e && e.preventDefault ) e.preventDefault(); return false; };

/**
 * Find nearest parent
 * @param {Element} node Node to find parent from.
 * @param {(Function|string)} condition Matching condition, if string then it is selector (slow), if function then it is predicate.
 * @returns {Element|null} Matching parent element, or null if not found.
 */
_.parent = function _parent ( node, condition ) {
   if ( ! node ) return node;
   if ( typeof( condition ) === 'string' ) {
      var list = _.array( _( condition ) );
      condition = function ( e ) { return list.indexOf( e ) >= 0; };
   } else if ( condition === undefined )
      condition = function (){ return true; };

   do {
      node = node.parentNode;
   } while ( node !== null && ! condition( node ) );
   return node;
};

/**
 * Create a DOM element and set its attributes / contents.
 *
 * Creating 'script' should also set the async attribute, whether to enable or prevent async execution.
 *
 * @param {string} tag Tag name of element to create.
 * @param {(Object|string)=} attr Text content String, or object with properties to set. e.g. text, html, class, onclick, disabled, style, etc.
 * @returns {Element} Created DOM element.
 */
_.create = function _create ( tag, attr ) {
   /* Disabled Id/class parsing because just the check cause a slow down of 6% to 12%, and does not do anything new. *
   if ( typeof( attr ) !== 'object' ) attr = { 'text' : attr }; // Convert text / numeric attribute to proper attribute object
   if ( tag.indexOf( '#' ) > 0  || tag.indexOf( '.' ) > 0 ) { // Parse 'table.noprint.fullwidth#nav' into tag, class, id
      if ( ! attr ) attr = {}; // Create attribute object if not given
      tag.split( /(?=[#.])/ ).forEach( function( e, i ) {
         if ( i === 0 ) return tag = e; // Set first token as tag name
         if ( e.charAt(0) === '#') return attr.id = e.substr(1); // Set id
         if ( ! attr.className ) return attr.className = e.substr(1); // Set className
         attr.className += ' ' + e.substr(1); // Append className
      } );
   } /**/
   var result = document.createElement( tag );
   if ( attr ) {
      if ( typeof( attr ) !== 'object' ) {
         result.textContent = attr;
      } else {
         _.attr( result, attr );
      }
   }
   return result;
};

/**
 * Convert selector / DOM element / NodeList / array of Node to array-like node list.
 *
 * @param {(string|Node|NodeList|Array)} e Selector or element(s).
 * @returns {(NodeList|Array)} Array-like list of e.
 */
_.domList = function _domList ( e ) {
   if ( typeof( e ) === 'string' ) return _( e );
   else if ( e instanceof Element || e.length === undefined || e instanceof Window ) return [ e ];
   return e;
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// DOM Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Set a list of object's DOM attribute to set to given value.
 * Can also mass add event listener.
 * Please use _.css to set specific inline styles.
 *
 * @param {(string|Node|NodeList|Array)} ary Element selcetor, dom list, or Array of JS objects.
 * @param {(Object|string)} obj DOM attribute or attribute object to set.  text = textContent, html = innerHTML, class = className, onXXX = XXX addEventListener.
 * @param {*=} value Value to set.  If 'undefined' then will delete the attribute.
 * @returns {Array} Array-ifed ary
 */
_.attr = function _attr( ary, obj, value ) {
   var attr = obj;
   if ( _.is.literal( obj ) ) {
      attr = {};
      attr[ obj ] = value;
   }
   ary = _.domList( ary );
   _.forEach( ary, function _attr_each( e ) {
      for ( var name in attr ) {
         var val = attr[ name ];
         switch ( name ) {
            case 'text':
            case 'textContent' :
               e.textContent = attr.text;
               break;

            case 'html':
            case 'innerHTML' :
               e.innerHTML = attr.html;
               break;

            case 'class' :
            case 'className' :
               e.className = val;
               break;

            case 'parent' :
            case 'parentNode' :
               if ( val && val.appendChild )
                  val.appendChild( e );
               else if ( ! val && e.parentNode )
                  e.parentNode.removeChild( e );
               break;

            case 'child' :
            case 'children' :
               while ( e.firstChild ) e.removeChild( e.firstChild );
               if ( val ) _.forEach( _.domList( val ), function _attr_each_child ( child ) {
                  if ( child ) e.appendChild( child );
               } );
               break;

            case 'style' :
               if ( typeof( val ) === 'object' ) {
                  _.style( e, val );
                  break;
               }
               // Else fall through as set/remove style attribute

            default:
               if ( name.substr( 0, 2 ) === 'on' ) {
                  e.addEventListener( name.substr( 2 ), val );
               } else if ( val !== undefined ) {
                  e.setAttribute( name, val );
               } else {
                  e.removeAttribute( name );
               }
         }
      }
   } );
   return ary;
};

/**
 * Set a list of object's style's attribute/property to same value
 *
 * @param {(string|Node|NodeList|Array)} ary Element selcetor, dom list, or Array of JS objects.
 * @param {(Object|string)} obj Style attribute or attribute object to set.
 * @param {*=} value Value to set.  If 'undefined' then will also delete the style attribute.
 * @returns {Array} Array-ifed ary
 */
_.style = function _style ( ary, obj, value ) {
   var attr = obj;
   if ( typeof( attr ) === 'string' ) {
      attr = {};
      attr[ obj ] = value;
   }
   ary =_.domList( ary );
   _.forEach( ary, function _styleEach ( e ) {
      for ( var name in attr ) {
         if ( attr[ name ] !== undefined ) {
            e.style[ name ] = attr[ name ];
         } else {
            e.style[ name ] = '';
            delete e.style[ name ];
         }
      }
   } );
   return ary;
};

/**
 * Show DOM elements by setting display to ''.
 * Equals to _.style( e, 'display', undefined )
 *
 * @param {(string|Node|NodeList|Array)} e Selector or element(s).
 * @returns {Array} Array-ifed e
 */
_.show = function _show ( e ) { return _.style( e, 'display', undefined ); };

/**
 * Hide DOM elements by setting display to 'none'.
 * Equals to _.style( e, 'display', 'none' )
 *
 * @param {(string|Node|NodeList|Array)} e Selector or element(s).
 * @returns {Array} Array-ifed e
 */
_.hide = function _show ( e ) { return _.style( e, 'display', 'none' ); };

/**
 * Set DOM elements visibility by setting display to '' or 'none.
 *
 * @param {(string|Node|NodeList)} e Selector or element(s).
 * @param {boolean} visible If true then visible, otherwise hide.
 * @returns {Array} Array-ifed e
 */
_.visible = function _visible ( e, visible ) {
   return visible ? _.show( e ) : _.hide( e );
};

/**
 * Clear a DOM elements of all children.
 *
 * @param {(string|Node|NodeList)} e Selector or element(s).
 * @returns {Array} Array-ifed e
 */
_.clear = function _clear ( e ) {
   e = _.domList( e );
   _.forEach( e, function _clear_each ( p ) {
      var c = p.firstChild;
      while ( c ) {
         p.removeChild( c );
         c = p.firstChild;
      }
   } );
   return e;
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Internationalisation and localisation
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Get language string and, if additional parameters are provided, format the parameters
 *
 * @param {string} path Path of resource to get
 * @param {*=} defaultValue Default value to use if target resource is unavailable.
 * @param {...*} param Parameters to replace %1 %2 %3 etc.
 */
_.l = function _l ( path, defaultValue, param /*...*/ ) {
   var l = _.l;
   var result = l.getset( l.currentLocale, path, undefined );
   if ( result === undefined ) result = defaultValue !== undefined ? defaultValue : path;
   if ( arguments.length > 2 ) {
      if ( arguments.length === 3 ) return l.format( ""+result, param );
      else return l.format.apply( this, [ result ].concat( _.ary( arguments, 2 ) ) );
   }
   return result;
};

/**
 * Format a string by replacing %1 with first parameter, %2 with second parameter, etc.
 *
 * @param {string} input String to format
 * @param {...*} param Parameters
 * @returns {string}
 */
_.l.format = function _l_format ( input, param /*...*/ ) {
   for ( var i = 1, l = arguments.length ; i < l ; i++ )
      input = input.replace( '%'+i, arguments[i] );
   return input;
};

/** Current locale for set and get operations. */
_.l.currentLocale = 'en';

/** Fallback locale in case localisation is not found */
_.l.fallbackLocale = 'en';

/** L10n resources. */
_.l.data = _.map();

/** Storage key for saving / loading locale */
_.l.saveKey = 'sparrow.l.locale';

/**
 * Set current locale.
 *
 * @param {string} lang  Locale to use. Pass in empty string, null, false etc. to use auto-detection
 */
_.l.setLocale = function _l_setLocale ( lang ) {
    if ( ! lang ) return;
    if ( lang === _.l.currentLocale ) return;
    _.l.currentLocale = lang;
};

/**
 * Override auto detect locale.
 *
 * @param {string} lang  Locale to use and save.
 */
_.l.saveLocale = function _l_saveLocale ( lang ) {
   if ( lang ) {
      _.pref.set( _.l.saveKey, lang );
      _.l.setLocale( lang );
   } else {
      _.pref.set( _.l.saveKey ); // Delete preference
   }
};

/**
 * Detect user locale.  Check saved locale if exist, otherwise check browser language setting.
 *
 * @param {string=} defaultLocale  Default locale to use.
 * @return {string} Current locale after detection.
 */
_.l.detectLocale = function _l_detectLocale ( defaultLocale ) {
   var l = _.l, pref = _.pref( _.l.saveKey, _.get( window, 'navigator', [ 'language', 'userLanguage' ] ) );
   if ( defaultLocale ) l.fallbackLocale = defaultLocale;
   if ( pref ) l.setLocale( _.l.matchLocale( pref, Object.keys( l.data ) ) );
   return l.currentLocale;
};

/**
 * Given a target locale, return a full or partial match from candidates.
 * Match is case-insensitive.
 *
 * @param {string} target Target locale to match.
 * @param {Array} candidates Result candidates.
 * @return {(string|null)} Matched candidate locale, must be at least a partial match, otherwise null.
 */
_.l.matchLocale = function _l_matchLocale ( target, candidates ) {
   if ( candidates.indexOf( target ) >= 0 ) return target; // Exact match short circuit
   // Try full match
   target = target.toLowerCase();
   var list = candidates.map( function( e ){ return e.toLowerCase(); } );
   var pos = list.indexOf( target );
   if ( pos >= 0 ) return candidates[ pos ];
   // Try partial match
   list = list.map( function( e ){ return e.split('-')[0]; } );
   pos = list.indexOf( target.split( '-' )[ 0 ] );
   if ( pos >= 0 ) return candidates[ pos ];
   return null;
};

/**
 * Get/set l10n resource on given path
 *
 * @param {string} path Path to get/set resource.
 * @param {*} set  Resource to set.  If null then regarded as get.
 * @param {string} locale Locale to use. NO DEFAULT.
 * @returns {*} if set, return undefined.  If get, return the resource.
 */
_.l.getset = function _l_getset ( locale, path, set ) {
   var p = [ locale ], l = _.l;
   if ( path ) p = p.concat( path.split( '.' ) );
   var last = p.pop();
   var base = l.data;
   // Explore path
   for ( var i = 0, len = p.length ; i < len ; i++ ) {
      var node = p[i];
      if ( base[node] === undefined )
         if ( set === undefined ) return;
         else base[node] = _.map();
      base = base[node];
   }
   // Set or get data
   if ( set !== undefined ) {
      base[last] = set;
   } else {
      if ( base[last] === undefined && locale !== l.fallbackLocale ) return l.getset( l.fallbackLocale, path, undefined );
      return base[last];
   }
};

/**
 * Set l10n resource on given path
 *
 * @param {string} path Path to set resource
 * @param {*} data Resource to set
 */
_.l.set = function _l_set ( locale, path, data ) {
   if ( arguments.length == 2 ) {
      data = path;
      path = locale;
      locale = _.l.currentLocale;
   }
    _.l.getset( locale, path, data );
};

/**
 * Localise all child elements with a class name of 'i18n' using its initial textContent or value as resource path.
 *  e.g. <div class='i18n'> gui.frmCalcluate.lblHelp </div>
 *
 * @param {Node=} root Root element to localise, default to whole document
 */
_.l.localise = function _l_localise ( root ) {
   if ( root === undefined ) {
      root = document.documentElement;
      var title = _.l( 'title', null );
      if ( typeof( title ) === 'string' ) document.title = title;
   }
   root.setAttribute( 'lang', _.l.currentLocale );
   _.forEach( _( root, ".i18n" ), function _l_localise_each ( e ) {
      var key = e.getAttribute( "data-i18n" );
      if ( ! key ) {
         switch ( e.tagName.toUpperCase() ) {
            case 'INPUT':    key = e.value;
                             break;
            case 'LINK' :    key = e.getAttribute( 'title' );
                             break;
            case 'MENUITEM': key = e.getAttribute( 'label' );
                             break;
            default:         key = e.textContent;
         }
         if ( key ) key = key.trim();
         if ( ! key ) {
            e.classList.remove( 'i18n' );
            return _.warn( 'i18 class without l10n key: ' + e.tagName.toLowerCase() + (e.id ? '#' + e.id : '' ) + ' / ' + e.textContext );
         }
         key = key.trim();
         e.setAttribute( "data-i18n", key );
      }
      var val = _.l( key, key.split('.').pop() );
      switch ( e.tagName.toUpperCase() ) {
         case 'INPUT':    e.value = val;
                          break;
         case 'LINK' :    e.setAttribute( 'title', val );
                          break;
         case 'MENUITEM': e.setAttribute( 'label', val );
                          break;
         default:         e.innerHTML = val;
      }
   } );
};

_.info('[Sparrow] Sparrow loaded.');
_.time();

return _;

}).call( window || global || this );