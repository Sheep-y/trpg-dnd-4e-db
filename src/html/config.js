/*
 * config.js
 * Path Configurations
 */

od.config = {
   "show_download" : location.protocol === 'file:' && window.ActiveXObject && /\bMSIE (9|\d\d+)\./.test(navigator.userAgent),
   "simulate" : false, // If true, load data from local file instead of live url
   "simulated_data_url" : 'debug_files',
   "data_read_path" : 'offline_database_files',
   "data_write_path" : location.href.replace( /file:\/\/\/|[^\\\/]+.html?(\?.*)?$/g, '' ) + 'offline_database_files',
   "retry" : 3,
   "retry_interval" : 30 * 1000,
   "down_interval" : 0,
   "thread" : 6,
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
         function url( category ) { return od.config.data_read_path + '/'+category+'/_listing.js'; },
      "extended" :
         function url( category ) { return od.config.data_read_path + '/'+category+'/_extended.js'; },
      "index" :
         function url( category ) { return od.config.data_read_path + '/'+category+'/_index.js'; },
      "data" :
         function url( category, id ) { return od.config.data_read_path + '/'+category+'/'+id.replace( /\.aspx\?id=|\W+/g, '' )+'.js'; }
   },
   "file" : {
      "catalog" :
         function url() { return od.config.data_write_path+'/catalog.js'; },
      "raw" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_listing.js'; },
      "extended" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_extended.js'; },
      "index" :
         function url( category ) { return od.config.data_write_path + '/'+category+'/_index.js'; },
      "data" :
         function url( category, id ) { return od.config.data_write_path + '/'+category+'/'+id.replace( /\.aspx\?id=|\W+/g, '' )+'.js'; }
   }
};