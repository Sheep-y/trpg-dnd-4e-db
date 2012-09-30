<script type='text/javascript'>'use strict';
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

   updateCountdown : 0,
   newItem : [ /* ['Sample','sample001'],['Sample','sample02']*/ ],
   changedItem : [],
   deleteItem : [],
   itemCount : 0,

   /** Get category listing. Call get_category for each category. */
   get_index: function downloader_get_index() {
      var address = oddi.debug ? 'data/debug/test-search.xml' : 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords=jump&nameOnly=false&tab=Glossary';
      _.cor( address,
         function( data, xhr ){
            var remote = oddi.downloader.remote = {};
            var tabs = _.xml(xhr.responseText).getElementsByTagName("Tab");
            oddi.downloader.updateCountdown = tabs.length;
            for ( var i = 0 ; i < tabs.length ; i++ ) {
               var category = tabs[i].getElementsByTagName('Table')[0].textContent;
               oddi.downloader.get_category( category );
            }
         } );
   },

   /** Get entry listing of a category */
   get_category: function downloader_get_category( cat ) {
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
            if ( --oddi.downloader.updateCountdown == 0 ) oddi.downloader.find_changed();
         } );
   },

   find_changed : function downloader_find_changed() {
      var newItem = [];
      var changedItem = [];
      var deletedItem = [];
      var itemCount = 0;

      var data = oddi.data;
      var remote = oddi.downloader.remote;
      var find = oddi.data.find_in_list;

      // Scan for new / changed items
      for ( var cat in remote ) {
         var rlist = remote[cat].listing;
         itemCount += rlist.length;
         if ( data[cat] === undefined ) {
            // New category
            rlist.forEach( function dfc_nc(e){ newItem.push( [cat, e[0]] ); } );
         } else {
            // Existing category
            var category = data[cat];
            var rcategory = remote[cat];
            if ( rcategory.columns.toString() !== category.columns.toString() ) {
               // Category column changed
               rlist.forEach( function dfc_cc(e){ changedItem.push( [cat, e[0]] ); } );
            } else {
               // Scan for changes in column
               var list = category.listing;
               rlist.forEach( function dfc_ec(e){
                  if ( find( list, e[0] ).toString() !== e.toString() ) changedItem.push( [cat, e[0]] );
               } );
            }
         }
      }
      // Scan for removed items
      for ( var cat in data ) {
         var list = data[cat].listing;
         if ( remote[cat] === undefined ) {
            deletedItem.push( [cat, null] );
         } else {
            var rlist = remote[cat].listing
            list.forEach( function dfc_ecd(e){
               if ( find( rlist, e[0] ).toString() !== e.toString() ) deleteItem.push( [cat, e[0]] );
            } );
         }
      }
      oddi.downloader.newItem = newItem;
      oddi.downloader.changedItem = changedItem;
      oddi.downloader.deletedItem = deletedItem;
      oddi.downloader.itemCount = itemCount;
      oddi.action.download.show_update_buttons();
   }
}

</script>