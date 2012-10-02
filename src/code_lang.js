<script type='text/javascript'>'use strict';
/*
 * code_lang.js
 * Localization code
 */

/** Get language string and, if additional parameters are provided, format the parameters */
_.l = function _l( path ) {
   var result = _.l.getset( path );
   if ( !result ) return path;
   if ( arguments.length > 1 )
      for ( var i = 1 ; i < arguments.length ; i++ )
         result = result.replace( '%'+i, arguments[i] );
   return result;
};

/** Current locale for set and get operations. */
_.l.currentLocale = 'en';
/** L10n resources. */
_.l.data = {};

/** Set current locale. */
_.l.setLocale = function _l_setLocale( l ) { _.l.currentLocale = l; }

/** Get/set l10n resource on given poth */
_.l.getset = function _l_getset( path, set ) {
   var p = path.split(/\./);
   var last = p.pop();
   p.unshift( _.l.currentLocale );
   var base = _.l.data;
   // Explore path
   p.forEach(function(node){
      if ( base[node] === undefined ) base[node] = {};
      base = base[node];
   });
   // Set data
   if ( set !== undefined ) base[last] = set;
   return base[last];
}

/** Set l10n resource on given poth */
_.l.set = function _l_set( path, data ) {
   _.l.getset( path, data );
   return _;
}

/** Localise all child elements with a class name of 'i18n' using its initial textContent as path */
_.l.localise = function _l_localise( root ) {
   var el = root.getElementsByClassName( "i18n" );
   for ( var i = 0 ; i < el.length ; i++ ) {
      var e = el[i];
      var key = e.getAttribute("data-i18n");
      if ( !key ) e.setAttribute("dataset-i18n", key = e.textContent.trim() );
      e.innerHTML = _.l( key );
   }
}

</script>