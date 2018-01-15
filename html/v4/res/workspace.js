(function data_init( global ) { "use strict";

function log () {
   console.log.apply( console, arguments );
}

global.onconnect = function( me ) {
   const port = me.ports[0];

   port.onmessage = function ( e ) {
      log( e.data );
      port.postMessage([ "Bar" ]);
   };
}

})( this );