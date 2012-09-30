<script>'use strict';
/*
 * data_downloader.js
 * Core (non-ui) logic of entry downloading
 */

// GUI namespace
oddi.downloader = {
   /** Remote listing. Just for finding out what needs to be updated, does not contain index or details. */
   remote: {
      // Sample: { columns: [ "Id", "Name", "Category", "Source" ], listing: [ [,,,], [,,,] ] }
   },

   /** Get category listing. Call get_category for each category. */
   get_index: function download_get_index() {
      var address = oddi.debug ? 'data/debug/test-search.xml' : 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords=jump&nameOnly=false&tab=Glossary';
      _.cor( address,
         function( data, xhr ){
            var remote = oddi.downloader.remote = {};
            var tabs = _.xml(xhr.responseText).getElementsByTagName("Tab");
            for ( var i = 0 ; i < tabs.length ; i++ ) {
               var category = tabs[i].getElementsByTagName('Table')[0].textContent;
               oddi.downloader.get_category( category );
            }
         }
      );
   },

   /** Get entry listing of a category */
   get_category: function download_get_category( cat ) {
      var address = oddi.debug ? ( 'data/debug/search-'+cat+'.xml' ) : ( 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords=&nameOnly=false&tab='+cat );
      _.cor( address,
         function( data, xhr ){
            var result = _.xml(xhr.responseText).getElementsByTagName("Results")[0];
            var current = result.firstElementChild;
            if ( current !== null ) {
               var remote = oddi.downloader.remote;
               var category = remote[ cat ] = {};
               // Get columns
               var prop = current.getElementsByTagName('*');
               var info = [];
               var col = prop.length;
               for ( var i = 0 ; i < col ; i++ ) info.push( prop[i].tagName );
               category.columns = info;
               // Get listing
               var list = category.listing = [];
               while ( current !== null ) {
                  prop = current.getElementsByTagName('*');
                  info = new Array( col );
                  for ( var i = 0 ; i < col ; i++ ) info[i] = prop[i].textContent;
                  list.push( info );
                  current = current.nextElementSibling;
               }
            }
         }
      );
   },
}

</script>