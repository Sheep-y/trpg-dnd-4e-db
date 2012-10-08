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
   xhr.open( 'POST', url );
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

_.js = function _js( url, onload ) {
   var e = document.createElement( 'script' );
   e.src = url;
   _.info( "Script: "+url);
   // Optional callback
   if ( onload ) e.addEventListener( 'load', onload );
   // Cleanup - remove script node so that saved html wouldn't be polluted
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
   msg = timeToStr() + " " + msg;
   if ( window.console && console[type] ) console[type]( msg );
   return msg;
};
_.info = function _info( msg ) { _.log( 'info', msg ); };
_.warn = function _info( msg ) { _.log( 'warn', msg ); };
_.error = function _info( msg ) { alert( _.log( 'error', msg ) ); };


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
      e.id = '_temp_html';
      e.style.display = 'none';
      document.body.appendChild(e);
   }
   e.innerHTML = txt;
   return e;
}
_.html.node = null;

/** Get current time in H:M:S.MS format */
function timeToStr() {
   var t = new Date();
   return "["+t.getHours()+":"+t.getMinutes()+":"+t.getSeconds()+"."+t.getMilliseconds()+"]";
}

</script><noscript>
   <h1> Please enable JavaScript </h1>
   <h1> 請啟用 JavaScript </h1>
</noscript>