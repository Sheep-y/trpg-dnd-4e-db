<script>'use strict';
/*
 * code_util.js
 * Utility codes, including initial detection of browser feature and no script.
 */

// Main namespace
var oddi = {};
 
// Simple check for browser features
if ( ! document.querySelectorAll || !window.Storage ) {
   alert('Please upgrade browser.');
}
 
function _( cssSelector ) {
   return document.querySelectorAll( cssSelector );
}

_.ajax = function _ajax( url, data, onsuccess, onfail, ondone ) {
   var xhr = new XMLHttpRequest();
   xhr.open( url, 'POST' );
   //xhr.mozBackgroundRequest = true;
   xhr.onreadystatechange = function _ajax_onreadystatechange() {
      if ( xhr.readyState == 4 ) {
         if ( [200,302].indexOf( xhr.status ) >= 0 ) {
            if ( onsuccess ) onsuccess(xhr);
         } else {
            if ( onfail ) onfail(xhr);
         }
         if ( ondone ) ondone(xhr);
      }
   }
   xhr.send();
   return xhr;
}

_.js = function _js( url ) {
   var e = document.createElement( 'script' );
   e.classNAme = 'temp';
   e.src = url;
   document.body.appendChild( e );
}

</script><noscript>
   <h1> Please enable JavaScript </h1>
   <h1> 請啓用 JavaScript </h1>
</noscript>