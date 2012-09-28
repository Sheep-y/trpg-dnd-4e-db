<script>'use strict';
/*
 * code_lang.js
 * Localization code
 */

_.l = function( path, data ) {
   var p = path.split(/\./);
   var last = p.pop();
   p.unshift( _.l.currentLocale );
   var base = _.l.data;
   var len = p.length;
   // Explore path
   p.forEach(function(node){
      if ( base[node] === undefined ) base[node] = {};
      base = base[node];
   });
   if ( data === undefined ) {
      var result = base[last];
      return result !== undefined ? result : path;
   }
   // Set data
   base[last] = data;
   return _;
};

_.l.currentLocale = 'en';
_.l.data = {};

_.l.setLocale = function _l_setLocale( l ) {
   _.l.currentLocale = l;
}

_.l.localise = function _l_localise( root ) {
   var el = root.getElementsByClassName( "i18n" );
   for ( var i = 0 ; i < el.length ; i++ ) {
      var e = el[i];
      var key = e.dataset["i18n"];
      if ( !key ) e.dataset["i18n"] = key = e.textContent.trim();
      e.innerHTML = _.l( key );
   }
}

</script>