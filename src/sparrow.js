<script type='text/javascript'>'use strict';
/*
 * code_util.js
 * Utility codes, including initial detection of browser feature and no script.
 */

// Simple check for browser features
if ( ! document.querySelectorAll || !window.Storage ) {
   alert('Please upgrade browser.');
}

function _( cssSelector ) {
   return document.querySelectorAll( cssSelector );
}

/**
 * Ajax function
 */
_.ajax = function _ajax( url, onsuccess, onfail, ondone, xhr ) {
   if ( xhr === undefined ) xhr = new XMLHttpRequest();
   _.info( "Ajax: "+url);
   xhr.open( 'GET', url );
   //xhr.mozBackgroundRequest = true;
   xhr.onreadystatechange = function _ajax_onreadystatechange() {
      if ( xhr.readyState == 4 ) {
         // 0 is a possible response code for local file access under IE 9 ActiveX
         if ( [0,200,302].indexOf( xhr.status ) >= 0 && xhr.responseText ) {
            if ( onsuccess ) onsuccess.call( xhr, xhr.responseText, xhr );
         } else {
            if ( onfail ) onfail.call( xhr, xhr );
         }
         if ( ondone ) ondone.call( xhr, xhr );
      }
   };
   xhr.send();
   return xhr;
};

/**
 * Load a javascript from an url
 */
_.js = function _js( url, onload, charset ) {
   var e = document.createElement( 'script' );
   e.src = url;
   if ( charset ) e.setAttribute( 'charset', charset );
   _.info( "Script: "+url);
   // Optional callback
   if ( onload ) e.addEventListener( 'load', onload );
   // Cleanup - remove script node so that saved html wouldn't be polluted.
   // Separate from onload so that we runs even if onload thrown error.
   e.addEventListener('load', function(){ document.body.removeChild(e) });
   document.body.appendChild( e );
};

/**
 * Cross Origin Request function
 */
_.cor = function _cor( url, onsuccess, onfail, ondone ) {
   if ( window.ActiveXObject !== undefined ) {
      return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   } else {
      _.info( "Cor: "+url);
      alert('Please override Cross Origin Request control');
      // enablePrivilege is disabled since Firefox 15
      //try {
      //   netscape.security.PrivilegeManager.enablePrivilege('UniversalXPConnect');
      //} catch ( e ) {
      //   alert(e);
      //}
      //return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   }
};

_.log = function _info( type, msg ) {
   if ( msg === undefined ) {
      msg = type;
      type = 'log';
   }
   if ( window.console && console[type] ) {
      var t = new Date();
      console[type]( "["+t.getHours()+":"+t.getMinutes()+":"+t.getSeconds()+"."+t.getMilliseconds()+"] " + msg );
   }
   return msg;
};
_.info = function _info( msg ) { _.log( 'info', msg ); };
_.warn = function _info( msg ) { _.log( 'warn', msg ); };
_.error = function _info( msg ) {
   if ( ! _.error.timeout ) {
      _.error.timeout = setTimeout( function(){
         _.error.timeout = 0;
         var err = _.error.log;
         _.error.log = ""
         alert( err );
      }, 50 );
   }
   _.error.log += _.log( 'error', msg ) + "\n";
};
_.error.timeout = 0;
_.error.log = "";

_.escHtml = function( t ) { return t.replace( _.escHtml.regxLt, '&lt;').replace( _.escHtml.regxAmp, '&amp;'); };
_.escHtml.regxLt = /</g;
_.escHtml.regxAmp = /&/g;

_.escJs = function( t ) { return t.replace( _.escJs.regxLf, '\\n').replace( _.escJs.regxEsc, '\\$0'); };
_.escJs.regxLf = /\r?\n/g;
_.escJs.regxEsc = /'"/g;

/**
 * parse xml and return an xml document
  */
_.xml = function _xml( txt ) {
   if ( window.DOMParser !== undefined ) {
      return new DOMParser().parseFromString( txt, 'text/xml' );
   } else if ( window.ActiveXObject !== undefined )  {
      var xml = new ActiveXObject('Msxml2.DOMDocument.3.0');
      xml.loadXML( txt );
      return xml;
   } else {
      alert('XML Parser not supported');
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
}
_.html.node = null;


/**
 * Countdown Latch object
 */
_.Latch = function( countdown, ondone ) {
   if ( typeof( countdown ) === 'function' ) {
      ondone = countdown;
      countdown = 0;
   }
   //if ( !ondone ) throw "IllegalParameterException: Latch callback must not be empty";
   this.count = countdown;
   this.ondone = ondone;
}
_.Latch.prototype = {
   count : 0,
   ondone : null,
   count_up : function latch_countup(){ ++this.count; },
   count_down : function latch_countdown(){
      if ( --this.count < 0 ) throw "IllegalStateException: Latch count below zero";
      if ( this.count == 0 ) if ( this.ondone ) this.ondone();
   }
}

</script><noscript>
   <h1> Please enable JavaScript </h1>
   <h1> 請啟用 JavaScript </h1>
</noscript>