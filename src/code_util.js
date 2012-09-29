<script>'use strict';
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
   }
   xhr.send();
   return xhr;
}

_.js = function _js( url, onload ) {
   var e = document.createElement( 'script' );
   e.src = url;
   // Optional callback
   if ( onload ) e.addEventListener('load', onload);
   // Cleanup - remove script node so that saved html wouldn't be polluted
   e.addEventListener('load', function(){ document.body.removeChild(e) });
   document.body.appendChild( e );
}

/**
 * Cross Origin Request function
 */
_.cor = function _cor( url, onsuccess, onfail, ondone ) {
   if ( window.ActiveXObject !== undefined ) {
      return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   } else {
      alert('Please override Cross Origin Request control');
      // enablePrivilege is disabled since Firefox 15
      //try {
      //   netscape.security.PrivilegeManager.enablePrivilege('UniversalXPConnect');
      //} catch ( e ) {
      //   alert(e);
      //}
      //return _.ajax( url, onsuccess, onfail, ondone, new ActiveXObject("Microsoft.XMLHttp") );
   }
}

/**
 * parse xml
  */
_.xml = function _xml( txt ) {
   if ( window.DOMParser !== undefined ) {
      return new DOMParser().parseFromString( txt, 'text/xml' );
   } else if ( window.ActiveXObject !== undefined )  {
      // TODO: implement activex
   } else {
      alert('XML Parser not supported');
   }
}

</script><noscript>
   <h1> Please enable JavaScript </h1>
   <h1> 請啟用 JavaScript </h1>
</noscript>