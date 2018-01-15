(function top_init( global ) { "use strict";

const ns = global.od = {};

function isStr ( obj ) { return typeof obj === "string"; }
function isObj ( obj ) { return typeof obj === "object"; }

function find ( css ) {
   return document.querySelector( css );
}

function findAll ( css ) {
   return document.querySelectorAll( css );
}

function iter ( obj ) {
   return isObj( obj ) && Symbol.iterator in obj ? obj : [ obj ];
}

function iterElem ( obj ) {
   return isStr( obj ) ? findAll( obj ) : iter( obj );
}

function log () {
   console.log.apply( console, arguments );
}


function addEvent ( element, event, handler, options ) {
   for ( const type of iter( event ) )
      for ( const elem of iterElem( element ) )
         for ( const func of iter( handler ) )
            elem.addEventListener( type, func, options );
}


document.body.classList.remove( "noscript" );


(function nav_init( global ) {  const nav = find( "nav" );

   addEvent( "#btn_menu", "click", () => {
      nav.className = "opened";
      nav.setAttribute( "aria-hidden", "false" );
   });

   addEvent( "#menu_close", "click", () => {
      nav.className = "closed";
      nav.setAttribute( "aria-hidden", "true" );
   });

})();

var myWorker = new SharedWorker( "res/workspace.js" );

myWorker.onerror = ( e ) => log( e );

myWorker.port.onmessage = ( e ) => {
   log( 'Message received from worker: ' + e.data );
}

myWorker.port.postMessage([ "Foo" ]);


})( window );