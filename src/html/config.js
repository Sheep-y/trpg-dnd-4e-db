/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * config.js
 *
 * System Configurations. Most are not configurable during runtime.
 */

od.config = {
   "simulate" : false, // If true, load data from local file instead of live url
   "simulated_data_url" : 'offline_simulation_files',
   "data_read_path" : '4e_database_files',
   "data_write_path" : location.href.replace( /file:\/\/\/|[^\\\/]+\.html?(\?.*)?$/g, '' ) + '4e_database_files',
   "data_write_compress" : true, // Whether to compress listing and index, the two biggest files.
   "retry" : 3, // Number of retry of download.
   "retry_interval" : 30 * 1000, // (ms) Interval between retry.
   "down_interval" : 0, // (ms) Interval between download, but unlikely to work......
   "thread" : 6, // Number of download threads

   "url_monitor_interval" : 500, // Duration between checking url change, in ms.

   "source" : {
      "catalog" :
         function config_source_catalog () {
            if ( od.config.simulate ) return od.config.simulated_data_url+'/test-search.xml';
            var keyword = ['jump','prone','dazed','knowledge','acrobatics','endurance','fly','vision','light','cover','concealment','swim','detect magic'];
            keyword = keyword[Math.floor(Math.random()*keyword.length)];
            return 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords='+keyword+'&nameOnly=false&tab=Glossary';
         },
      "list" :
         function config_source_list ( category ) {
            return od.config.simulate
               ? ( od.config.simulated_data_url+'/search-' + category + '.xml' )
               : ( 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab='+category );
         },
      "xsl" :
         function config_source_xsl ( category ) {
            return od.config.simulate
               ? ( od.config.simulated_data_url+'/xsl-' + category + '.xsl' )
               : ( 'http://www.wizards.com/dndinsider/compendium/xsl/' + category + '.xsl' );
         },
      "data" :
         function config_source_data ( category, itemId ) {
            return ( od.config.simulate ? od.config.simulated_data_url : 'http://www.wizards.com/dndinsider/compendium' ) + '/' + itemId  ;
         }
   },
   "url" : {
      "catalog" :
         function config_url () { return od.config.data_read_path + '/catalog.js'; },
      "raw" :
         function config_url ( category ) { return od.config.data_read_path + '/' + category + '/_raw.js'; },
      "listing" :
         function config_url ( category ) { return od.config.data_read_path + '/' + category + '/_listing.js'; },
      "index" :
         function config_url ( category ) { return od.config.data_read_path + '/' + category + '/_index.js'; },
      "data" :
         function config_url ( category, id ) { return od.config.data_read_path + '/' + category + '/' + od.config.id( id ) + '.js'; }
   },
   "file" : {
      "catalog" :
         function config_file () { return od.config.data_write_path+'/catalog.js'; },
      "raw" :
         function config_file ( category ) { return od.config.data_write_path + '/' + category + '/_raw.js'; },
      "listing" :
         function config_file ( category ) { return od.config.data_write_path + '/' + category + '/_listing.js'; },
      "index" :
         function config_file ( category ) { return od.config.data_write_path + '/' + category + '/_index.js'; },
      "data" :
         function config_file ( category, id ) { return od.config.data_write_path + '/' + category + '/' + od.config.id( id ) + '.js'; }
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
         case 'paragon': return 11;
         case 'epic'   : return 21;
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
   ],

   "symbol_conversion" : 'Common',
   "symbols": {
      "Common" : {
         '⋖': '☄',
         '͜͡⋖': '(☄)',
         '⚔': '☭',
         '͜͡⚔': '(☭)',
         '͜͡➶': '(➶)',
         '͜͡✻': '(✻)',
         '☼': '❂'
      },
      "Dingbat" : {
         '✦': '★',
         '⋖': '<',
         '⚔': '†',
         '͜͡⋖': '(<)',
         '͜͡⚔': '(†)',
         '͜͡➶': '(➶)',
         '͜͡✻': '(✻)',
         '☼': '❂'
      },
      "Plain" : {
         '✦': '+',
         '⋖': '<',
         '͜͡⋖': '(<)',
         '⚔': '+',
         '͜͡⚔': '(+)',
         '➶': '~',
         '͜͡➶': '(~)',
         '✻': 'X',
         '͜͡✻': '(X)',
         '⚀': '[1]',
         '⚁': '[2]',
         '⚂': '[3]',
         '⚃': '[4]',
         '⚄': '[5]',
         '⚅': '[6]',
         '☼': '(O)'
      }
   }
};