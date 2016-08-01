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
         function config_url ( category ) { return od.config.data_read_path + '/' + category.toLowerCase() + '/_index.js'; },
      "data" :
         function config_url ( category, id ) { return od.config.data_read_path + '/' + category.toLowerCase() + '/' + od.config.id( id ) + '.js'; }
   },
   "id" : function config_id ( id ) {
      return id.replace( /\.aspx\?id=|\W+/g, '' );
   },
   "display_columns" : function  config_display_columns ( cols ) {
      if ( typeof( cols ) === 'string' ) return cols.substr( cols.length - 4 ) !== 'Sort';
      return cols.filter( od.config.display_columns );
   },
   "level_to_int" : function config_level_to_int ( data ) {
      if ( ! data ) return 0;
      switch ( data.toLowerCase() ) {
         case 'heroic' : return 1;
         case 'paragon': return 10.5;
         case 'epic'   : return 20.5;
      }
      return +data.replace( /\D+/g, '' );
   },

   "category_order" : [
        "#LightGray",
      "{All}",
      "Glossary",
        "#LightBlue",
      "Race",
      "Background",
      "Theme",
        "#Gold",
      "Class",
      "ParagonPath",
      "EpicDestiny",
        "#Coral",
      "Power",
      "Feat",
      "Item",
      "Ritual",
        "#LightPink",
      "Companion",
      "Deity",
      "Poison",
        "#LightGreen",
      "Disease",
      "Monster",
      "Terrain",
      "Trap",
        "#LightGrey" // Default for remaining categories undefined here
   ]
};