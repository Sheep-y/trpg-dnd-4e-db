/*
 * config.js
 * System Configurations
 */

od.config = {
   "title_prefix" : "Compendium - ",

   "simulate" : false, // If true, load data from local file instead of live url
   "simulated_data_url" : 'offline_simulation_files',
   "data_read_path" : '4e_database_files',
   "data_write_path" : location.href.replace( /file:\/\/\/|[^\\\/]+.html?(\?.*)?$/g, '' ) + '4e_database_files',
   "retry" : 3, // Number of retry of download.
   "retry_interval" : 30 * 1000, // (ms) Interval between retry.
   "down_interval" : 0, // (ms) Interval between download, but unlikely to work......
   "thread" : 6, // Number of download threads
   "source" : {
      "catalog" :
         function url() {
            if ( od.config.simulate ) return od.config.simulated_data_url+'/test-search.xml';
            var keyword = ['jump','prone','dazed','knowledge','acrobatics','endurance','fly','vision','light','cover','concealment','swim','detect magic'];
            keyword = keyword[Math.floor(Math.random()*keyword.length)];
            return 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords='+keyword+'&nameOnly=false&tab=Glossary';
         },
      "list" :
         function url( category ) {
            return od.config.simulate
               ? ( od.config.simulated_data_url+'/search-'+category+'.xml' )
               : ( 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab='+category );
         },
      "xsl" :
         function url( category ) {
            return od.config.simulate
               ? ( od.config.simulated_data_url+'/xsl-'+category+'.xsl' )
               : ( 'http://www.wizards.com/dndinsider/compendium/xsl/'+category+'.xsl' );
         },
      "data" :
         function url( category, itemId ) {
            return ( od.config.simulate ? od.config.simulated_data_url : 'http://www.wizards.com/dndinsider/compendium' ) + '/' + itemId  ;
         }
   },
   "url" : {
      "catalog" :
         function url() { return od.config.data_read_path + '/catalog.js'; },
      "raw" :
         function url( category ) { return od.config.data_read_path + '/'+category+'/_raw.js'; },
      "listing" :
         function url( category ) { return od.config.data_read_path + '/'+category+'/_listing.js'; },
      "index" :
         function url( category ) { return od.config.data_read_path + '/'+category+'/_index.js'; },
      "data" :
         function url( category, id ) { return od.config.data_read_path + '/'+category+'/'+id.replace( /\.aspx\?id=|\W+/g, '' )+'.js'; }
   },
   "file" : {
      "catalog" :
         function url() { return od.config.data_write_path+'/catalog.js'; },
      "raw" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_raw.js'; },
      "listing" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_listing.js'; },
      "index" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_index.js'; },
      "data" :
         function url( category, id ) { return od.config.data_write_path + '/'+category+'/'+id.replace( /\.aspx\?id=|\W+/g, '' )+'.js'; }
   },

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