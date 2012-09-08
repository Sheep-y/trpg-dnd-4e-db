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

</script><noscript>
   <h1> Please enable JavaScript </h1>
   <h1> 請啓用 JavaScript </h1>
</noscript>