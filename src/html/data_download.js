/*
 * data_download.js
 * Core (non-ui) logic of entry downloading
 */

// GUI namespace
od.download = {
   /** Remote listing. Just for finding out what needs to be updated, does not contain index or details. */
   "category": {
      // Sample: {
      //    name: "Sample",
      //    count: 32,
      //    columns: [ "Id", "Name", "Category", "Source" ],
      //    listing: [ [,,,], [,,,] ],
      //    dirty: [ "id1","id2" ]
      // }
   },
   "get" : function download_get( name ) {
      if ( name === undefined ) {
         var result = [];
         for ( var c in this.category ) result.push( this.category[c] );
         return result;
      }
      return this.category[name];
   },

   /**
    * Get category listing.
    */
   "get_catalog": function download_get_catalog( onload, onerror ) {
      var address = od.config.source.catalog();
      _.cor( address,
         function download_get_catalog_callback( data, xhr ){
            var category = od.download.category = {};
            var tabs = _.xml(xhr.responseText).getElementsByTagName("Tab");
            for ( var i = 0, len = tabs.length ; i < len ; i++ ) {
               var cat = tabs[i].getElementsByTagName('Table')[0].textContent;
               category[cat] = new od.download.RemoteCategory( cat );
            }
            _.call( onload );
         },
         onerror ? onerror : function(){ _.error( "Cannot download catalog from " + address ); }
      );
   },

   "executor" : new _.Executor( 1, od.config.down_interval ),
   "login_check_id": -1,

   /**
    * Schedule download of a list of items in same category.
    *
    * @param {type} remote Remote category object
    * @param {type} list Array of id to download
    * @param {type} onprogress
    * @returns {undefined}
    */
   "schedule_download": function download_schedule_download( remote, list, onprogress ) {
      var down = od.download;
      var exec = down.executor;
      var local = od.data.create( remote.name );
      local.check_columns( remote.raw_columns );
      if ( local.count ) {
         local.load_index( download_schedule_download_run ); // Make sure index is loaded, since we may be doing an update.
      } else {
         download_schedule_download_run(); // New or resetted category, no need to load index.
      }
      function download_schedule_download_run() {
         _.debug('Schedule '+ list.length);
         list.forEach( function download_schedule_download_each( item ) {
            //var retry = 0;
            function download_schedule_download_task( threadid ) {
               if ( ! item ) {
                  _.call( onprogress, remote, item );
                  return true;
               }
               remote.get_data(
                  item,
                  function download_schedule_download_ondown( remote, id, data ){
                     if ( data.toLowerCase().indexOf( "subscrib" ) >= 0 && data.toLowerCase().indexOf( "password" ) >= 0 ) {
                        if ( down.login_check_id < 0 || down.login_check_id === threadid ) {
                           // If need to login, and if we are first, show login dialog in model mode
                           exec.pause();
                           down.login_check_id = threadid;
                           if ( confirm( _.l( 'action.download.msg_login' ) ) ) {
                              window.showModalDialog( od.config.source.data( remote.name, item ) );
                              download_schedule_download_task( threadid );
                           } else {
                              exec.clear();
                              exec.finish( threadid );
                           }
                        } else {
                           //--down.login_check_count;
                           exec.asap( download_schedule_download_task );
                           exec.finish( threadid );
                        }
                     } else {
                        // Success download
                        exec.thread = od.config.thread;
                        if ( down.login_check_id >= 0 ) {
                           down.login_check_id = -1;
                           exec.resume();
                        }
                        var index = _.col( remote.raw, 0 ).indexOf( id );
                        local.update( id, remote.raw[index], data  );
                        if ( remote.dirty.indexOf( id ) < 0 ) remote.dirty.push( id );
                        exec.finish( threadid );
                        _.call( onprogress, remote, id );
                     }
                  },
                  function download_schedule_download_onerror( cat, id, err ){
                     /** Get data already include retry.
                     if ( ++retry <= od.config.retry ) {
                        setTimeout( function(){
                           exec.asap( download_schedule_download_run );
                        }, od.config.retry_interval  );

                     } else { */
                        var i = _.col( cat.raw ).indexOf( id );
                        i = i ? ' (' + cat.raw[i][1] + ')' : '';
                        _.error( _.l( 'error.updating_data', null, id + i , cat.name, err ) );
                     //}
                     exec.finish( threadid );
                     _.call( onprogress, remote, id );
                  }
               );
               // Return false make sure the thread pool knows we're not finished (if we are already finished, we have already notified it)
               return false;
            }
            exec.add( download_schedule_download_task );
         });
      }
   }
};

od.download.RemoteCategory = function RemoteCategory( name ) {
   this.name = name;
   this.title = _.l( 'data.category.' + name, name );
   this.raw_columns = [];
   this.raw = [];
   this.dirty = [];
   this.changed = [];
   this.added = [];
   //this.loading = [];
};
od.download.RemoteCategory.prototype = {
   "name": "",
   "title": "",
   "state" : "unlisted", // "unlisted" -> "listing" <-> "listed" -> "downloading" -> "downloaded" -> "saved"
   "progress" : "", // Text statue progress

   /** Raw data used to compose list property */
   "raw_columns": [],  // e.g. ["ID","Name","Category","SourceBook", ... ]
   "raw": [],          // e.g. [ ["sampleId001","Sample Data 1","Sample1","Git"], ... ]

   "dirty": [],        // e.g. [ "sampleId001", "sampleId002" ]

   "count": false,
   "changed": [],
   "added": [],

   //"loading": [],

   /**
    * Get entry listing of this category. Catalog must have been loaded.
    *
    * @param {type} onload Success load callback.
    * @param {type} onerror Failed load callback.
    * @param {type} retry Retry countdown, first call should be undefined.  Will recursively download until negative.
    * @returns {undefined}
    */
   "get_listing": function download_Cat_get_listing( onload, onerror ) {
      var remote = this;
      var data, xsl;
      var latch = new _.Latch( 3 );
      var err = _.callonce( onerror );
      this.get_remote( od.config.source.list( this.name ), function(txt){ data = txt; latch.count_down(); }, err );
      this.get_remote( od.config.source.xsl ( this.name ), function(txt){ xsl  = txt; latch.count_down(); }, err );
      latch.ondone = function download_Cat_get_listing_done() {
         remote.added = [];
         remote.changed = [];
         remote.count = 0;
         var list = remote.raw = [];
         var done = true;

         if ( ! data || ! xsl ) return err('No Data');

         data = data.replace( /’/g, "'" );
         xsl = xsl.replace( /\n\s+/g, '\n' );
         xsl = xsl.replace( /<script(.*\r?\n)+?<\/script>/g, '' ); // Remove scripts so that xsl can run
         xsl = xsl.replace( /<xsl:sort[^>]+>/g, '' ); // Undo sort so that transformed id match result
         xsl = xsl.replace( /Multiple Sources/g, '<xsl:apply-templates select="SourceBook"/>' ); // Undo multiple source replacement
         xsl = xsl.replace( /\bselect="'20'"\s*\/>/, 'select="\'99999\'"/>' ); // Undo paging so that we get all result
         var results = _.xml( data ).querySelectorAll('Results > *');
         var transformed = _.xsl( data, xsl );
         var idList = [];

         if ( results.length > 0 && transformed !== null ) {
            remote.raw_columns = _.col( results[0].getElementsByTagName('*'), 'tagName' );

            var ids = _.ary( _.xpath( transformed.documentElement, '//div//td[1]/a' ) ).map( function(e){ return e.getAttribute('href'); } );
            if ( ids.length !== results.length ) _.error('Error getting listing for ' + remote.title + ': xsl transform rows mismatch' );
            for ( var i = 0, l = ids.length ; i < l ; i++ ) {
               var rowId = ids[i];
               if ( rowId ) { // Skip empty and duplicate id - which is download link
                  rowId = rowId.trim();
                  var row = _.col( results[i].getElementsByTagName('*'), 'textContent' );
                  if ( idList.indexOf( rowId ) >= 0 ) {
                     _.error( "Duplicate result: " + rowId + " (" + row[1] + ")" );
                     continue;
                  }
                  row[0] = rowId;
                  for ( var j = 1, rl = row.length ; j < rl ; j++ ) row[j] = row[j].trim(); // Remove unnecessary whitespaces
                  list.push( row );
                  idList.push( rowId );
               }
            }
            // Check local exist and columns match, if ok then pass to find_changed()
            remote.count = list.length;
            var local = od.data.get( remote.name );
            if ( local !== null ) {
               // Load local index and find differences
               local.load_listing( function() {
                  remote.find_changed();
                  _.call( onload, remote, remote );
               } );
               done = false;
            } else {
               remote.added = _.col( list );
            }
         }
         if ( done ) _.call( onload, remote, remote );
      };
      latch.count_down();
   },


   "get_remote": function download_Cat_get_remote( url, onload, onerror, retry ) {
      var remote = this;
      if ( retry === undefined ) retry = od.config.retry;
      if ( onerror && typeof( onerror ) === 'string' ) onerror = function() { _.error( _.l.format( onerror, remote.name, url ) ); };
      _.cor( url, onload, function download_Cat_get_remote_error(){
            --retry;
            if ( retry <= 0 ) {
               _.call( onerror, remote, remote );
            } else setTimeout( function download_Cat_get_remote_retry(){ remote.get_remote( url, onload, onerror, retry ); }, od.config.retry_interval );
         }
      );
   },

   "find_changed": function download_Cat_find_changed( ) {
      var local = od.data.get( this.name );
      this.added = [];
      this.changed = [];
      if ( local === null )
         return this.added = _.col( this.raw );
      if ( JSON.stringify( this.raw_columns ) !== JSON.stringify( local.raw_columns ) )
         return this.changed = _.col( this.raw );
      for ( var i = 0, l = this.raw.length ; i < l ; i++ ) {
         var row = this.raw[i], id = row[0];
         var localrow;
         local.raw.some( function(e){ if ( e[0] === id ) return localrow = e; return false;  } );
         if ( !localrow ) {
            this.added.push( id );
         } else {
            if ( JSON.stringify( row ) !== JSON.stringify( localrow ) ) this.changed.push( id );
         }
      }
   },

   /**
    * Get entry content
    */
   "get_data": function download_Cat_get_data( id, onload, onerror, retry ) {
      var address = od.config.source.data( this.name, id );
      var remote = this;
      if ( retry === undefined ) retry = od.config.retry;
      _.cor( address,
         function download_Cat_get_data_callback( data, xhr ){
            if ( ! data ) return downloar_Cat_get_data_retry( xhr, 'No Data' );
            _.call( onload, remote, remote, id, xhr.responseText );
         },
         downloar_Cat_get_data_retry
      );
      function downloar_Cat_get_data_retry( xhr, err ){
         _.warn( 'downloar_Cat_get_data_retry: ' + retry );
         --retry;
         if ( retry <= 0 ) {
            onerror ? _.call( onerror, remote, remote, id, err ) : _.error("Cannot download " + remote.name + " listing from " + address + ": " + err );
         } else remote.get_data( id, onload, onerror, retry );
      }
   },

   "save" : function download_Cat_save( ondone, onerror ) {
      if ( this.dirty.length ) { // Has dirty
         var remote = this;
         var local = od.data.get( remote.name );
         local.save_listing( function download_Cat_saved_list(){
            local.save_index( function download_Cat_saved_index(){ 
               var latch = new _.Latch( remote.dirty.length+1 );
               remote.dirty.forEach( function download_Cat_save_data(id) {
                  local.save_data( id, latch.count_down_function(), onerror );
               });
               latch.ondone = function download_Cat_saved_data() {
                  remote.state = "saved";
                  _.call( ondone, remote );
               };
               latch.count_down();
            }, onerror);
         }, onerror);
      } else {
         _.call( ondone, this );
      }
   }
};
_.seal( od.data.Category.prototype );
