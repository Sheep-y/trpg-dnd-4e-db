/*
 * sparrow.js
 *
 * Sparrow - light weight JS library.
 *
 * @author Hermia Yeung (c) 2013
 */

// Simple check for browser features
if ( ! document.querySelectorAll || !window.Storage ) {
   alert('Please upgrade browser.');
}

// CSS select only but very simple and fast.
// Only guarantees to return an array-like object.
function _( root, selector ) {
   if ( selector === undefined ) {
      selector = root;
      root = document;
   }
   if ( selector.indexOf(' ') > 0 || ! /^[#.]?\w+$/.test( selector ) ) {
      return root.querySelectorAll( selector );
   } else {
      // Get Element series is many times faster then querySelectorAll
      if ( selector.indexOf('#') === 0 ) {
         var result = root.getElementById( selector.substr(1) );
         return result ? [ result ] : [ ];
      }
      if ( selector.indexOf('.') === 0 ) return root.getElementsByClassName( selector.substr(1) );
      return root.getElementsByTagName( selector );
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Array Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert an array-like object to be an array. Default to clone or slice.
 * 
 * @param {Array-like object} subject Subject to be converted.
 * @param {Integer} startpos If given, work like Array.slice( startpos ).
 * @param {Integer} endpos If this and startpos is given, work like Array.slice( startpos, length ).
 * @returns {Array} Clone or slice of subject.
 */
_.ary = function _ary( subject, startpos, endpos ) {
   if ( subject.length <= 0 ) return [];
   var s = Array.prototype.slice;
   if ( startpos === undefined ) return subject instanceof Array ? subject : s.call( subject, 0 );
   return s.call( subject, startpos, endpos );
};

/**
 * Given an array-like object and one or more columns, extract and return those columns from subject.
 * 
 * @param {Array-like object} subject Array-like object to be extracted.
 * @param {String} column Columns (field) to extract.
 * @returns {Array} Array (if single column) or Array of Array (if multiple columns).
 */
_.col = function _col( subject, column /* ... */) {
   if ( ! ( subject instanceof Array ) ) subject = _.ary( subject );
   if ( column === undefined ) return subject.map(function(e){ return e[0]; });
   if ( arguments.length === 2 ) return subject.map(function(e){ return e[column]; });
   else return subject.map(function(e){ 
      var result = [];
      for ( var i = 1, l = arguments.length ; i < l ; i++ ) result.push( e[arguments[i]] );
      return result ;
   });
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Function Helpers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Call a function - if it is not undefined - in a try block and return its return value.
 *
 * @param {Function} func   function to call. Must be function, null, or undefined.
 * @param {Object} thisObj  'this' to be passed to the function
 * @param {any} param       call parameters, can have multiple.
 * @returns {any}           Return value of called function, or undefined if function is not called or has error.
 */
_.call = function _call( func, thisObj, param /*...*/ ) {
   if ( func === undefined || func === null ) return undefined;
   try {
      if ( arguments.length <= 1 ) return func();
      else if ( arguments.length <= 3 ) return func.call( thisObj, param );
      else return func.apply( thisObj, _.ary(arguments, 2) );
   } catch ( e ) { _.error(e); }
};

/**
 * Given a function, return a function that when called multiple times, only the first call will be transferred.
 * Useful for concurrent error callback, so that the first error pass through and subsequency error doesn't.
 * Parameters passed to the returned function will be supplied to the callback as is.
 * This function will disregard any additional parameters.
 * 
 * @param {type} func  Function to call.
 * @returns {function} Function that can be safely called multiple times without calling func more then once
 */
_.callonce = function _call( func ) {
   if ( !func ) return function(){};
   return function _callonce_call () {
      if ( !func ) return;
      var f = func;
      func = null;
      return _.call.apply( this, [ f, this ].concat( _.ary( arguments ) ) );
   };
};

/**
 * Capture parameters in a closure and return a callback function
 * that can be called at a later time.
 */
_.callparam = function _callparam( param /*...*/, func ) {
   var l = arguments.length-1;
   var f = arguments[l];
   var arg = _.ary( arguments, 0, l );
   var thisp = this;
   return function _callback() { f.apply( thisp, arg ); };
};

/**
 * Capture parameters in a closure and return a callback function
 * that can be called at a later time.
 */
_.callfunc = function _callfunc( func, param /*...*/ ) {
   var arg = _.ary( arguments, 1 );
   var thisp = this;
   return function _callback() { func.apply( thisp, arg ); };
};

if ( window.setImmediate === undefined ) {
   if ( window.requestAnimationFrame ) {
      window.setImmediate = window.requestAnimationFrame;
      window.clearImmediate = window.cancelAnimationFrame;       
   } else {
      window.setImmediate = function(func){ return window.setTimeout(func, 0); };
      window.clearImmediate = window.clearTimeout;
   }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Ajax / js / Cross origin inclusion
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Ajax function
 */
_.ajax = function _ajax( url, onsuccess, onfail, ondone, xhr ) {
   if ( xhr === undefined ) xhr = new XMLHttpRequest();
   _.info( "[AJAX] Ajax: "+url);
   xhr.open( 'GET', url );
   //xhr.mozBackgroundRequest = true;
   var finished = false;
   xhr.onreadystatechange = function _ajax_onreadystatechange() {
      if ( xhr.readyState === 4 ) {
         _.debug( 'Ajax ready 4, status ' + xhr.status + ', url ' + url );
         // 0 is a possible response code for local file access under IE 9 ActiveX
         if ( [0,200,302].indexOf( xhr.status ) >= 0 && xhr.responseText ) {
            setImmediate( function _ajax_err_status(){
               if ( finished ) return;
               finished = true;
               _.call( onsuccess, xhr, xhr.responseText, xhr );
               _.call( ondone, xhr, xhr );
            } );
         } else {
            setImmediate( function _ajax_err_status(){
               if ( finished ) return;
               finished = true;
               _.call( onfail, xhr, xhr, "HTTP Response Code " + xhr.status );
               _.call( ondone, xhr, xhr );
            } );
         }
         xhr.onreadystatechange = function(){}; // Avoid repeated call
      }
   };
   try {
      xhr.send();
   } catch (e) {
      _.error( 'Ajax exception on ' + url + ': ' + e );
      finished = true;
      _.call( onfail, xhr, xhr, e );
      _.call( ondone, xhr, xhr );
   }
   return xhr;
};

/**
 * Load a javascript from an url
 */
_.js = function _js( url, option ) {
   if ( option === undefined ) option = {};
   // Validate before doing anything, if pass then we are done
   if ( option.validate && option.validate.call( null, url, option ) ) return _js_done( 'onload' );

   var e = document.createElement( 'script' );
   e.src = url;
   if ( option.charset ) e.charset = option.charset;
   _.info( "[JS] Load script: " + url );

   var done = false;
   function _js_done( call, log ) {
      if ( done ) return;
      done = true;
      if ( log ) _.log( log );
      _.call( option[call], e, url, option );
      if ( e && e.parentNode === document.body ) document.body.removeChild(e);
   }

   e.addEventListener( 'load', function _js_load(){
      // Delay execution to make sure validate/load is called _after_ script has been ran.
      setImmediate( function _js_load_delayed() {
         // Loaded _will_ run regardless of whether the script has error, as long as it exists.
         if ( option.validate ) {
            var ok;
            try { ok = option.validate.call( e, url, option ); } catch(ex) { _.error(ex); }
            if ( !ok ) {
               return _js_done( 'onerror', "[JS] Script error: " + url );
            }
         }
         _js_done( 'onload', "[JS] Script loaded: " + url );
      } );
   } );
   e.addEventListener( 'error', function _js_error(){
      _js_done( 'onerror', "[JS] Script not found: " + url );
   } );

   document.body.appendChild( e );
};

/**
 * Cross Origin Request function
 */
_.cor = function _cor( url, onsuccess, onfail, ondone ) {
   if ( window.ActiveXObject !== undefined ) {
      // XMLHttp can cross origin.
      return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   } else {
      _.info( "[COR] Cross orig req: "+url );
      alert('Please override Cross Origin Request control (TODO: Explain better)');
      // enablePrivilege is disabled since Firefox 15
      //try {
      //   netscape.security.PrivilegeManager.enablePrivilege('UniversalXPConnect');
      //} catch ( e ) {
      //   alert(e);
      //}
      //return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Document parsing
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * parse xml and return an xml document
  */
_.xml = function _xml( txt ) {
   if ( window.DOMParser !== undefined ) {
      return new DOMParser().parseFromString( txt, 'text/xml' );
      
   } else if ( window.ActiveXObject !== undefined )  {
      var xml = new ActiveXObject('Msxml2.DOMDocument.6.0');
      xml.loadXML( txt );
      return xml;

   } else {
      _.error('XML Parser not supported');
   }
};

/**
 * parse html and return the containing dom node
 */
_.html = function _html( txt ) {
   var e = _.html.node;
   if ( !e ) {
      e = _.html.node = document.createElement('div');
      e.style.display = 'none';
      document.body.appendChild(e);
   }
   e.innerHTML = txt;
   return e;
};
_.html.node = null;


/**
 * Apply an xsl to xml and return the result of transform
 * 
 * @param {type} xml XML String or document to be transformed.
 * @param {type} xsl XSL String or document to transform xml.
 * @returns {Document} Transformed fragment root or null if XSL is unsupported.
 */
_.xsl = function _xsl( xml, xsl ) {
   var xmlDom = typeof( xml ) === 'string' ? _.xml( xml ) : xml;
   var xslDom = typeof( xsl ) === 'string' ? _.xml( xsl ) : xsl;
   if ( window.XSLTProcessor ) {
      var xsltProcessor = new XSLTProcessor();
      xsltProcessor.importStylesheet( xslDom );
      return xsltProcessor.transformToFragment( xmlDom, document );
      
   } else if ( xmlDom.transformNode ) {
      return xmlDom.transformNode( xslDom );

   } else if ( window.ActiveXObject )  {
         /* // This code has problem with special characters
         var xslt = new ActiveXObject("Msxml2.XSLTemplate");
         if ( typeof( xsl === 'string' ) ) { // Must use ActiveX free thread document as source.
            xslDom = new ActiveXObject('Msxml2.FreeThreadedDOMDocument.3.0');
            xslDom.loadXML( xsl );
         }
         xslt.stylesheet = xslDom;
         var proc = xslt.createProcessor();
         proc.input = xmlDom;
         proc.transform();
         return _.xml( proc.output );
      */
         xmlDom = new ActiveXObject('Msxml2.DOMDocument.6.0');
         xmlDom.loadXML( xml );
         xslDom = new ActiveXObject('Msxml2.DOMDocument.6.0');
         xslDom.loadXML( xsl );
         var result = new ActiveXObject('Msxml2.DOMDocument.6.0');
         result.async = false;
         result.validateOnParse = true;
         xmlDom.transformNodeToObject( xslDom, result );
         return result;
         
   } else {
      return null;
   }
};

_.xpath = function _xpath( node, path ) {
   var doc = node.ownerDocument;
   if ( doc.evaluate ) {
      return doc.evaluate( path, node, null, XPathResult.ANY_TYPE, null );
   } else {
      return doc.selectNodes( path );
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Console logging & timing.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_.log = function _info( type, msg ) {
   if ( msg === undefined ) {
      msg = type;
      type = 'log';
   }
   if ( window.console ) {
      if ( console[type] === undefined ) type = 'log';
      var t = new Date();
      console[type]( "["+t.getHours()+":"+t.getMinutes()+":"+t.getSeconds()+"."+t.getMilliseconds()+"] " + msg );
   }
};
_.debug = function _info( msg ) { _.log( 'debug', msg ); };
_.info = function _info( msg ) { _.log( 'info', msg ); };
_.warn = function _info( msg ) { _.log( 'warn', msg ); };
_.error = function _info( msg ) {
   if ( ! _.error.timeout ) {
      // Delay a small period so that errors popup together instead ofone by one
      _.error.timeout = setTimeout( function _error_timeout(){
         _.error.timeout = 0;
         var err = _.error.log;
         _.error.log = "";
         alert( err );
      }, 50 );
   }
   _.log( 'error', msg );
   msg = "" + msg;
   if ( msg !== _.error.lastmsg ) {
      _.error.lastmsg = msg;
      _.error.log += msg + "\n";
   }
};
_.error.timeout = 0;
_.error.log = "";

_.time = function _time( msg ) {
   var t = _.time;
   if ( msg === undefined ) {
      t.base = new Date();
      t.last = null;
      return;
   }
   var now = new Date();
   var fromBase = now - t.base;
   var fromLast = t.last ? ( ',+' + (now - t.last) ) : '';
   _.debug( msg + ' (+' + fromBase + fromLast + ')' );
   t.last = now;
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// String escape
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_.escHtml = function _escHtml( t ) { 
   if ( ! t.match( /[<&'"]/ ) ) return t;
   return t.replace( /&/g, '&amp;').replace( /</g, '&lt;').replace( /"/g, '&quot;' ).replace( /'/g, '&#39;' );
};

_.escJs = function _escJs( t ) { 
   return t.replace( /\r?\n/g, '\\n').replace( /'"/g, '\\$0'); 
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Object freezing
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_.freeze = function _freeze( o ) { return Object.freeze ? Object.freeze(o) : o; };
_.seal = function _seal( o ) { return Object.seal ? Object.seal(o) : o; };
_.noExt = function _noExt( o ) { return Object.preventExtensions ? Object.preventExtensions(o) : o; };

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// DOM manipulation
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_.create = function _create( tag, attr ) {
   var result = document.createElement( tag );
   if ( attr ) {
      if ( attr.text ) {
         result.textContent = attr.text;
         delete attr.text;
      }
      if ( attr.html ) {
         result.innerHTML = attr.html;
         delete attr.html;
      }
      for ( var name in attr ) {
         if ( name.indexOf('on') === 0 ) {
            result.addEventListener( name.substr(2), attr[name] );
         } else {
            result.setAttribute( name, attr[name] );
         }
      }
   }
   return result;
};

_.domlist = function _domlist(e) {
   if ( typeof( e ) === 'string' ) return _(e);
   else if ( e.length === undefined ) return [ e ];
   return e;
};

_.show = function _show( e ) {
   e = _.domlist(e);
   for ( var i = 0, l = e.length ; i < l ; i++ ) {
      e[i].style.display = '';
      delete e[i].style.display;
   }
};

_.hide = function _show( e ) {
   e = _.domlist(e);
   for ( var i = 0, l = e.length ; i < l ; i++ ) e[i].style.display = 'none';
};

_.visible = function _visible( e, visible ) {
   if ( visible ) _.show(e); else _.hide(e);
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Asynchronous programming
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Countdown Latch object
 */
_.Latch = function( countdown, ondone ) {
   if ( typeof( countdown ) === 'function' ) {
      ondone = countdown;
      countdown = 0;
   }
   this.count = countdown;
   this.ondone = ondone;
};
_.Latch.prototype = {
   "count" : 0,
   "ondone" : null,

   /** Count up. */
   "count_up" : function _latch_countup( value ) {
      if ( value === undefined ) value = 1;
      this.count += value;
   },

   /** Count down.  If count reach 0 then run ondone. */
   "count_down" : function _latch_countdown( value ) {
      if ( value === undefined ) value = 1;
      this.count -= value;
      if ( this.count < 0 ) throw "IllegalStateException: Latch count below zero";
      if ( this.count == 0 ) _.call( this.ondone, this );
   },
   /** Return a function that can be used to countdown this latch */
   "count_down_function" : function _latch_countdown_function( value ) {
      var latch = this;
      return function _latch_countdown_callback() { latch.count_down( value ); };
   }
};

_.Executor = function( thread, interval ) {
   if ( thread ) this.thread = thread;
   if ( interval ) this.interval = interval;
   this.running = [];
   this.waiting = [];
};
_.Executor.prototype = {
   "_paused": false,
   "_lastRun": 0,
   "thread": 1,
   "interval": 0,
   "running": [],
   "waiting": [],
   "add": function _executor_add ( runnable, param /*...*/ ) {
      this.addTo.apply( this, [ this.waiting.length ].concat( _.ary( arguments ) ) );
      return this;
   },
   "asap": function _executor_asap ( runnable, param /*...*/ ) {
      this.addTo.apply( this, [0].concat( _.ary( arguments ) ) );
      return this;
   },
   "addTo": function _executor_addTo ( index, runnable, param /*...*/ ) {
      if ( ! runnable.name ) runnable.name = runnable.toString().match(/^function\s+([^\s(]+)/)[1];
      _.debug('Queue task ' + runnable.name );
      var arg = [ runnable ].concat( _.ary( arguments, 2 ) );
      this.waiting.splice( index, 0, arg );
      this.notice();
      return this;
   },
   "finish" : function _executor_finish ( id ) {
      var r = this.running[id];
      _.debug('Finish task #' + id + ' ' + r[0].name );
      this.running[id] = null;
      this.notice();
      return this;
   },
   "clear": function _executor_clear() { this.waiting = []; },
   "pause": function _executor_pause() { this._paused = true; return this; },
   "resume": function _executor_resume() { this._paused = false; this.notice(); return this; },
   "notice": function _executor_notice() {
      if ( this._paused ) return this;
      var nextInterval = 0;
      var exe = this;
      for ( var i = 0 ; i < this.thread && this.waiting.length > 0 ; i++ ) {
         if ( !this.running[i] ) {
            var r = this.waiting.splice( 0, 1 )[0];
            this.running[i] = r;
            //_.debug('Schedule task #' + i + ' ' + r[0].name );
            setTimeout( _.callparam( i, r, function _Executor_run(ii,r){
               _.debug('Start task #' + ii + ' ' + r[0].name );
               _lastRun = new Date().getTime();
               try {
                  if ( r[0].apply( null, [ ii ].concat( r.slice(1) ) ) !== false ) exe.finish( ii );
               } catch ( e ) {
                  _.error( e );
                  exe.finish( ii );
               }
            } ), nextInterval += this.interval );
         }
      }
      return this;
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Helper objects
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

_.EventManager = function _EventManager( owner, events ) {
   this.owner = owner;
   var lst = {};
   for ( var i = 0, l = events.length ; i < l ; i++ ) {
      lst[events[i]] = null;
   }
   this.lst = _.noExt( lst );
};
_.EventManager.prototype = {
   "lst" : {},
   "owner" : null,
   "add" : function _EventManager_add( event, listener ) {
      if ( this.lst[event] === undefined ) throw new Error("Unknown event");
      if ( this.lst[event] === null ) this.lst[event] = [];
      this.lst[event].push( listener );
   },
   "remove" : function _EventManager_remove( event, listener ) {
      var lst = this.lst[event];
      if ( lst === undefined ) throw new Error("Unknown event");
      if ( lst === null ) return;
      var i = lst.indexOf( listener );
      if ( i ) {
         lst.splice( i, 1 );
         if ( lst.length < 0 ) this.lst[event] = null;
      }
   },
   "fire" : function _EventManager_remove( event ) {
      var lst = this.lst[event];
      if ( lst === undefined ) throw new Error("Unknown event");
      if ( lst === null ) return;
      var l = lst.length, param = _.ary( arguments, 1 );
      for ( var i = 0 ; i < l ; i++ ) {
         lst[i].apply( this.owner, param );
      }
   }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Internationalisation and localisation
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/** Get language string and, if additional parameters are provided, format the parameters */
_.l = function _l( path, defaultValue, param /*...*/ ) {
   var l = _.l;
   var result = l.getset( path, null, l.currentLocale );
   if ( !result ) result = ( defaultValue !== undefined || defaultValue === null ) ? defaultValue : path;
   if ( arguments.length > 2 ) {
      if ( arguments.length === 3 ) return l.format( result, param );
      else return l.format.apply( this, [result].concat( _.ary(arguments, 2) ) );
   }
   return result;
};

_.l.format = function _l_format( input, param /*...*/ ) {
   for ( var i = 1, l = arguments.length ; i < l ; i++ )
      input = input.replace( '%'+i, arguments[i] );
   return input;
};

/** Current locale for set and get operations. */
_.l.currentLocale = 'en';

/** Fallback locale in case localisation is not found */
_.l.fallbackLocale = 'en';

/** L10n resources. */
_.l.data = {};

/**
 * Set current locale. 
 * 
 * @param {String} lang  Locale to use. Pass in empty string, null, false etc. to use auto-detection
 * @returns {undefined}
 */
_.l.setLocale = function _l_setLocale( lang ) { 
    if ( ! lang ) return _.l.detectLocale();
    if ( lang === _.l.currentLocale ) return;
    _.l.currentLocale = lang;
    _.l.event.fire( 'locale', lang );
};

/**
 * Override auto detect locale. 
 * 
 * @param {String} lang  Locale to use and save.
 * @returns {undefined}
 */
_.l.saveLocale = function _l_saveLocale( lang ) { 
    _.l.setLocale( lang );
    if ( window.Storage ) localStorage['_.l.locale'] = lang;
};

/**
 * Detect user locale.  First check local session then check language setting.
 *
 * @param {String} defaultLocale  Default locale to use 
 * @returns {undefined}
 */
_.l.detectLocale = function _l_detectLocale( defaultLocale ) {
    var l = _.l;
    var list = Object.keys( l.data );
    if ( defaultLocale ) l.fallbackLocale = defaultLocale;
    var pref = navigator.language;
    if ( window.Storage ) pref = localStorage['_.l.locale'] || pref;
    if ( list.indexOf( pref ) >= 0 ) return l.setLocale( pref );
    if ( pref.indexOf('-') < 0 ) return;
    pref = pref.split( '-' )[0];
    if ( list.indexOf( pref ) >= 0 ) l.setLocale( pref );
};

/** 
 * Get/set l10n resource on given path 
 * 
 * @param {type} path Path to get/set resource.
 * @param {type} set  Resource to set.  If null then regarded as get.
 * @param {type} locale Locale to use. NO DEFAULT.
 * @returns {varialbe} if set, return undefined.  If get, return the resource.
 */
_.l.getset = function _l_getset( path, set, locale ) {
   var p = path.split( '.' );
   var last = p.pop();
   p.unshift( locale );
   var base = this.data;
   // Explore path
   for ( var i = 0, l = p.length ; i < l ; i++ ) {
      var node = p[i];
      if ( base[node] === undefined ) base[node] = {};
      base = base[node];
   };
   // Set or get data
   if ( set !== null ) {
      base[last] = set;
   } else {
      if ( base[last] === undefined && locale !== this.fallbackLocale ) return this.getset( path, null, this.fallbackLocale );
      return base[last];
   }
};

/** 
 * Set l10n resource on given path 
 * 
 * @param {type} path Path to set resource
 * @param {type} data Resource to set
 * @returns {undefined}
 */
_.l.set = function _l_set( path, data ) { 
    _.l.getset( path, data, _.l.currentLocale );
    _.l.event.fire( 'set', path, data );
};

/**
 * Localise all child elements with a class name of 'i18n' using its initial textContent or value as resource path.
 *  e.g. <div class='i18n'> gui.frmCalcluate.lblHelp </div>
 * 
 * @param {type} root Root element to localise, default to whole document
 * @returns {undefined}
 */
_.l.localise = function _l_localise( root ) {
   if ( root === undefined ) root = document.documentElement;
   var _l = _.l;
   var el = root.getElementsByClassName( "i18n" );
   for ( var i = 0, l = el.length ; i < l ; i++ ) {
      var e = el[i];
      var isInput = e.tagName === 'INPUT';
      var key = e.getAttribute("data-i18n");
      if ( ! key ) {
          key = ( isInput ? e.value : e.textContent ).trim();
          e.setAttribute("data-i18n", key );
      }
      var val = _l( key, key.split('.').pop() );
      e[ isInput ? 'value' : 'innerHTML' ] = val;
   }
};

_.l.event = new _.EventManager( _.l, ['set','locale'] );

_.debug('Sparrow loaded.');
_.time();
/*
var e = new _.Executor();
e.add( function(r){ console.log(r); }, 1 );
e.add( function(r){ console.log(r); }, 2 );
e.asap( function(r){ console.log(r); }, 3 );
*/