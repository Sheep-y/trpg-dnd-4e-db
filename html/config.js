/*
 * config.js
 *
 * System Configurations. Most are not configurable during runtime.
 */

od.config = {
   "data_read_path" : location.pathname.match( /\w+(?=\.htm)/ ) + '_files',

   //"url_monitor_interval" : 500, // Duration between checking url change, in ms.

   "url" : {
      "catalog" :
         function config_url () { return od.config.data_read_path + '/catalog.js'; },
      "listing" :
         function config_url ( category ) { return od.config.data_read_path + '/' + category.toLowerCase() + '/_listing.js'; },
      "index" :
         function config_url ( category ) {
            return od.config.data_read_path + '/' + ( category ? category.toLowerCase() + '/_index.js'
                                                               : 'index.js' ); },
      "data" :
         function config_url ( category, id ) {
            var matches = id.match( /(\d{1,2})$/ ) || [];
            matches[1] = ~~matches[1]; // Removes leading 0
            return od.config.data_read_path + '/' + category.toLowerCase() + '/data' + matches[1] + '.js';
         }
   },
   "level_to_int" : function config_level_to_int ( data ) {
      if ( ! data ) return 0;
      switch ( data.toLowerCase() ) {
         case 'heroic' : return 5.5;
         case 'paragon': return 15.5;
         case 'epic'   : return 25.5;
      }
      var digits = data.replace( /\D+/g, '' );
      return digits === '0' ? 0.5 : +digits;
   },

   "category_order" : [
        "#LightGray",
      "{All}",
      "glossary",
        "#LightBlue",
      "race",
      "background",
      "theme",
        "#Gold",
      "class",
      "paragonpath",
      "epicdestiny",
        "#Coral",
      "power",
      "feat",
      "ritual",
        "#LightGreen",
      "item",
      "weapon",
      "implement",
      "armor",
        "#LightPink",
      "companion",
      "deity",
      "poison",
        "#Tan",
      "disease",
      "monster",
      "terrain",
      "trap",
        "#LightGrey" // Default for remaining categories undefined here
   ]
};