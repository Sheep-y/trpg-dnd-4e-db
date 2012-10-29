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

   updateCountdown : null, // Latch object
   newItem : [ /* ['Sample','sample001'],['Sample','sample02']*/ ],
   changedItem : [],
   deletedItem : [],
   itemCount : 0,

   /**
    * Get category listing. Call get_category for each category.
    */
   get_index: function downloader_get_index( ) {
      var address;
      if ( oddi.config.debug ) {
         address = oddi.config.debug_url+'/test-search.xml';
      } else {
         var keyword = ['jump','prone','dazed','knowledge','acrobatics','endurance','fly','vision','light','cover','concealment'];
         keyword = keyword[Math.floor(Math.random()*keyword.length)];
         address = 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/KeywordSearch?Keywords='+keyword+'&nameOnly=false&tab=Glossary';
      }
      _.cor( address,
         function( data, xhr ){
            var remote = oddi.downloader.remote = {};
            var tabs = _.xml(xhr.responseText).getElementsByTagName("Tab");
            oddi.downloader.updateCountdown = new _.Latch( tabs.length, function get_index_ondone() {
               oddi.data.load_all_index( oddi.downloader.find_changed );
            });
            for ( var i = 0, len = tabs.length ; i < len ; i++ ) {
               var category = tabs[i].getElementsByTagName('Table')[0].textContent;
               if ( oddi.config.cat_filter && category != oddi.config.cat_filter ) {
                  oddi.downloader.updateCountdown.count_down();
                  continue;
               }
               oddi.downloader.get_category( category );
            }
         }, oddi.gui.ajax_error( address ) );
   },

   /** Get entry listing of a category */
   get_category: function downloader_get_category( cat ) {
      var address = oddi.config.debug ? ( oddi.config.debug_url+'/search-'+cat+'.xml' ) : ( 'http://www.wizards.com/dndinsider/compendium/CompendiumSearch.asmx/ViewAll?tab='+cat );
      _.cor( address,
         function( data, xhr ){
            var result = _.xml(xhr.responseText).getElementsByTagName("Results")[0];
            var current = result.firstElementChild;
            // If we have a result
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
            oddi.downloader.updateCountdown.count_down();
         }, oddi.gui.ajax_error( address ) );
   },

   /** Content retrival stack info */
   stack : {
      threadCount : 0,
      running : null, // Latch object
      paused : null, // true or false
      checker : null, // checker thread id
      loginWindow : null, // popup login windows id
      stack : [],
      thread : [],
   },

   /** Get everything in stack  */
   get : function downloader_run() {
      var stack = oddi.downloader.stack;
      var max = Math.min( stack.stack.length, oddi.config.thread );
      stack.running = new _.Latch( max, function downloader_stack_ondone(){
         // Catch the errors so that parent error handler will not be triggered to call the latch count down again
         try {
            oddi.downloader.find_changed()
            try {
               oddi.data.write();
            } catch ( e ) { _.error( e ); }
         } catch ( e ) { _.warn( e ); }
      });
      stack.threadCount = max;
      stack.thread = [];
      for ( var i = 0 ; i < max ; i++ ) {
         setTimeout( (function(i){
            return function(){ oddi.downloader.run_stack(i); };
         })(i), i*1000 );
      }
   },

   /** Get entry listing */
   run_stack : function downloader_run_stack( id, lastDelay ) {
      var status = oddi.downloader.stack;
      status.thread[id] = 0;

      function reschedule( time ){ status.thread[id] = setTimeout( function(){ downloader_run_stack( id, time ); }, time ) };

      if ( status.paused && status.checker !== id ) {
         // Paused and we are not the checking instance, sleep for a while
         return reschedule( 500+Math.random()*1500 );
      } else {
         /** Cannot check cross origin window. Check with normal request instead.
         if ( status.paused && status.loginWindow != null ) {
            // If login window is not closed, and address is still at login, wait a bit longer
            if ( ! status.loginWindow.closed && status.loginWindow.location.href.indexOf('login') >= 0 )
               return reschedule( 500+Math.random()*500 );
         }
         */
         var cat = status.stack[ id ];
         var model = oddi.data;
         if ( cat ) {
            function runNextStack () {
               if ( status.stack.length > status.threadCount ) {
                  var next = status.stack.splice( status.threadCount, 1 )[0]
                  status.stack[ id ] = next;
                  reschedule( 0 );
               } else {
                  status.stack[ id ] = null;
                  status.running.count_down();
               }
               // Reset paused status, if we are checker
               if ( status.paused && status.checker === id ) {
                  status.paused = status.checker = status.loginWindow = null;
               }
            }
            /*if ( !cat[1] ) {
               model.create_category( cat[0], oddi.downloader.remote[cat[0]].columns );
               runNextStack();
            } else {    */
            var itemId = cat[1][0];
            var lcat = cat[0].toLowerCase();
            var address = oddi.config.debug ? ( oddi.config.debug_url+'/'+lcat+'-'+itemId+'.html' ) : ( 'http://www.wizards.com/dndinsider/compendium/'+lcat+'.aspx?id='+itemId );
            _.cor( address,
               function( data, xhr ){
                  // Success, check result
                  if ( data.toLowerCase().indexOf( "subscrib" ) >= 0 && data.toLowerCase().indexOf( "password" ) >= 0 ) {
                     status.paused = true;
                     status.checker = id;
                     status.loginWindow = window.open( address, 'loginPopup' );
                     alert( _.l( 'action.download.msg_login' ) );
                     lastDelay = ! lastDelay ? 5000 : Math.min( 3*60*1000, lastDelay + 5000 );
                     return reschedule( lastDelay );
                  }
                  var remote = oddi.downloader.remote;
                  try {
                     var col = remote[cat[0]].columns;
                     model.create_category( cat[0], col ).update( itemId, col, cat[1], model.preprocess( data ), runNextStack );
                  } catch ( e ) {
                     _.error( _.l( 'error.updating_data', cat[1][1], cat[0], e ) );
                     runNextStack();
                  }
               }, function( data, xhr ){
                  // Failed, set pause and schedule for timeout retry
                  if ( !status.paused ) {
                     status.paused = true;
                     status.checker = id;
                  }
                  // Retry ... with a limit. Retry too much and we give up.
                  lastDelay = ! lastDelay ? 10000 : ( lastDelay + 10000 );
                  if ( lastDelay < 60*1000 ) {
                     reschedule( lastDelay );
                  } else {
                     runNextStack();
                  }
               } );
         } else {
            // Nothing to get. This is usually not right. If we are checker, reset pause status.
            _.warn( "Reterival thread "+id+" has nothing to get.");
            if ( status.paused && status.checker === id ) {
               status.paused = status.checker = status.loginWindow = null;
            }
         }
      }
   },

   /**
    * Find new, changed, and removed entries.
    * Results are stored in downloader object.
    */
   find_changed : function downloader_find_changed() {
      var data = oddi.data.category;
      var remote = oddi.downloader.remote;
      var find = oddi.data.find_in_list;
      var newItem = [];
      var changedItem = [];
      var deletedItem = [];
      var itemCount = 0;

      // Scan for new / changed items
      for ( var cat in remote ) {
         if ( oddi.config.cat_filter && cat != oddi.config.cat_filter ) continue;
         var rlist = remote[cat].listing;
         itemCount += rlist.length;
         if ( !data || data[cat] === undefined ) {
            // New category
            rlist.forEach( function dfc_nc(e){ newItem.push( [cat, e] ); } );
         } else {
            // Existing category
            var category = data[cat];
            var rcategory = remote[cat];
            if ( rcategory.columns.toString().toUpperCase() !== category.columns.toString().toUpperCase() ) {
               // Category column changed
               //changedItem.push( [cat, null ] );
               rlist.forEach( function dfc_cc(e){ changedItem.push( [cat, e] ); } );
            } else {
               // Scan for changes in column
               var list = category.listing;
               rlist.forEach( function dfc_ec(e){
                  var local = find( list, e[0] );
                  if ( local < 0 ) newItem.push( [cat, e] );
                  else {
                     local = list[local];
                     if ( local.toString() !== e.toString() ) changedItem.push( [cat, e] );
                  }
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
               if ( find( rlist, e[0] ) < 0 ) deletedItem.push( [cat, e] );
            } );
         }
      }
      oddi.downloader.newItem = newItem;
      oddi.downloader.changedItem = changedItem;
      oddi.downloader.deletedItem = deletedItem;
      oddi.downloader.itemCount = itemCount;
      oddi.action.download.show_update_buttons();
   }
};

</script>