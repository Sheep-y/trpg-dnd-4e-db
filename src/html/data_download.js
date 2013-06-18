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
   dirty_catalog : false, // Mark whether master catalog is dirty, e.g. a category is deleted. Note that a dirty category always imply dirty catalog.
   "get" : function download_get ( name ) {
      var result;
      if ( name === undefined ) {
         result = [];
         for ( var c in this.category ) result.push( this.category[c] );
         return result;
      }
      result = this.category[name];
      return result ? result : null;
   },
           
   "create" : function download_create ( name ) {
      var result = this.get( name );
      if ( result === null ) this.category[name] = result = new od.download.RemoteCategory( name );
      return result;
   },

   "delete" : function download_delete ( remote ) {
      if ( ! od.data.category[ remote.name ] ) return _.error( "Cannot delete a non-local category" );
      switch ( remote.state ) {
         case "local" :
         case "absent" :
            delete this.category[ remote.name ];
            break;
         case "listing":
         case "downloading":
            _.error( "Cannot delete a listing / downloading category" );
            return;
         case "listed" :
            remote.added = _.col( remote.raw );
            remote.changed = [];
            remote.dirty = [];
            remote.reindexed = false;
            break;
         case "unlisted" :
      }
      delete od.data.category[ remote.name ];
      this.dirty_catalog = true;
   },

   /**
    * Get category listing.
    */
   "get_catalog": function download_get_catalog ( onload, onerror ) {
      var down = od.download;
      var address = od.config.source.catalog();
      _.cor( address,
         function download_get_catalog_callback( data, xhr ){
            // First reset state of all unlisted cat, assuming they are not on remote
            down.get().forEach( function download_get_catalog_reset( r ){
                if ( r.state === "unlisted" || r.state === "local" ) r.state = "absent";
            });
            // Get tab list and update state
            var tabs = _.xml(xhr.responseText).getElementsByTagName("Tab");
            for ( var i = 0, len = tabs.length ; i < len ; i++ ) {
               var r = down.create( tabs[i].getElementsByTagName('Table')[0].textContent );
               if ( r.state === 'absent' || r.state === 'local' ) r.state = 'unlisted';
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
    * @param {type} remote  Remote category object
    * @param {type} list    Array of id to download
    * @param {type} onstep  Callback on each progress
    * @returns {undefined}
    */
   "schedule_download": function download_schedule_download ( remote, list, onstep, ondone ) {
      remote.status = "downloading";
      var down = od.download;
      var exec = down.executor;
      var local = od.data.create( remote.name );
      local.check_columns( remote.raw_columns );
      if ( local.count ) {
         local.load_index( download_schedule_download_run ); // Make sure index is loaded, since we may be doing an update.
      } else {
         download_schedule_download_run(); // New or resetted category, no need to load index.
      }
      // Update progress now, because first item may be queued after other category
      var step = 0, total = list.length;
      remote.progress = _.l( 'action.download.lbl_progress', null, 0, total );
      _.call( onstep );
      function download_schedule_download_run() {
         _.debug( 'Schedule '+ list.length );
         var latch = new _.Latch( list.length, function download_schedule_download_done () {
            _.call( ondone, remote );
         });
         list.forEach( function download_schedule_download_each( item ) {
            //var retry = 0;
            function download_schedule_download_task( threadid ) {
               if ( ! item ) {
                  _.call( onstep, remote, item );
                  latch.count_down();
                  return true;
               }
               remote.get_data(
                  item,
                  function download_schedule_download_ondone ( remote, id, data ) {
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
                        remote.progress = _.l( 'action.download.lbl_progress', null, ++step, total );
                        exec.finish( threadid );
                        _.call( onstep, remote, id );
                        latch.count_down();
                     }
                  },
                  function download_schedule_download_onerror ( cat, id, err ){
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
                     _.call( onstep, remote, id );
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

od.download.RemoteCategory = function RemoteCategory ( name ) {
   this.name = name;
   this.title = _.l( 'data.category.' + name, name );
   this.dirty = [];
   this.reset();
};
od.download.RemoteCategory.prototype = {
   "name": "",
   "title": "",
   "state" : "local", // "local" -> "unlisted" OR "absent" -> "listing" <-> "listed" <-> "downloading"
   "progress" : "", // Text statue progress

   /** Raw data used to compose list property */
   "raw_columns": [],  // e.g. ["ID","Name","Category","SourceBook", ... ]
   "raw": [],          // e.g. [ ["sampleId001","Sample Data 1","Sample1","Git"], ... ]

   "dirty": [],        // e.g. [ "sampleId001", "sampleId002" ]

   "count": 0,
   "changed": [],
   "added": [],
   "reindexed" : false,
   
   "reset" : function download_Cat_reset () {
      this.raw_columns = [];
      this.raw = [];
      this.changed = [];
      this.added = [];
      this.progress = "";
   },

   //"loading": [],

   /**
    * Get entry listing of this category. Catalog must have been loaded.
    *
    * @param {type} onload Success load callback.
    * @param {type} onerror Failed load callback.
    * @param {type} retry Retry countdown, first call should be undefined.  Will recursively download until negative.
    * @returns {undefined}
    */
   "get_listing": function download_Cat_get_listing ( onload, onerror ) {
      this.state = "listing";
      var remote = this;
      var data, xsl;
      var latch = new _.Latch( 3 );
      var err = _.callonce( onerror );
      remote.state = _.l('action.download.lbl_fetching_both');
      this.get_remote( od.config.source.list( this.name ), function(txt){ 
         data = txt; 
         data = data.replace( /’/g, "'" );
         remote.state = _.l('action.download.lbl_fetching_xsl'); 
         latch.count_down(); 
      }, err );
      this.get_remote( od.config.source.xsl ( this.name ), function(txt){ 
         xsl = txt; 
         xsl = xsl.replace( /\n\s+/g, '\n' );
         xsl = xsl.replace( /<script(.*\r?\n)+?<\/script>/g, '' ); // Remove scripts so that xsl can run
         xsl = xsl.replace( /<xsl:sort[^>]+>/g, '' ); // Undo sort so that transformed id match result
         xsl = xsl.replace( /Multiple Sources/g, '<xsl:apply-templates select="SourceBook"/>' ); // Undo multiple source replacement
         xsl = xsl.replace( /\bselect="'20'"\s*\/>/, 'select="\'99999\'"/>' ); // Undo paging so that we get all result
         remote.state = _.l('action.download.lbl_fetching_xml');
         latch.count_down(); 
      }, err );
      latch.ondone = function download_Cat_get_listing_done () {
         remote.state = '';
         remote.added = [];
         remote.changed = [];
         remote.count = 0;
         var list = remote.raw = [];
         if ( ! data || ! xsl ) return err('No Data');

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
            remote.state = "listed";
            remote.count = list.length;
            remote.find_changed( onload );
         }
      };
      latch.count_down();
   },
           
   "get_remote": function download_Cat_get_remote ( url, onload, onerror, retry ) {
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

    /**
     * Check local exist and columns match, if ok then pass to remote.find_changed()
     * Oterwise assume all items are new.
     * 
     * @param {function } onload  Callback after changed items is founds
     * @returns {undefined}
     */
   "find_changed": function download_Cat_find_changed ( onload ) {
      var remote = this;
      var local = od.data.get( remote.name );
      var list = remote.raw;
      if ( local !== null ) {
         // Load local index and find differences
         local.load_raw( function download_Cat_find_changed_work () {
            var added = remote.added = [];
            var changed = remote.changed = [];
            var raw = remote.raw;
            var idList = _.col( local.raw );
            if ( JSON.stringify( remote.raw_columns ) !== JSON.stringify( local.raw_columns ) )
               return remote.changed = _.col( raw );
            for ( var i = 0, l = raw.length ; i < l ; i++ ) {
               var row = raw[ i ], id = row[ 0 ];
               var pos = idList.indexOf( id );
               if ( pos >= 0 ) {
                  if ( JSON.stringify( row ) !== JSON.stringify( local.raw[ pos ] ) ) changed.push( id );
               } else {
                  added.push( id );
               }
            }
            _.call( onload, remote );
         }, download_Cat_find_changed_addall );
      } else {
         download_Cat_find_changed_addall();
      }
      function download_Cat_find_changed_addall () {
         remote.added = _.col( list );
         _.call( onload, remote );
      }
   },

   "update_all" : function download_Cat_update_all ( onstep, ondone ) {
      var remote = this;
      remote.state = "downloading";
      od.download.schedule_download( remote, _.col( remote.raw ), onstep, function download_Cat_update_all_done () {
         remote.state = "listed";
         remote.find_changed( ondone );
      } );
   },

   "update_changed" : function download_Cat_update_changed ( onstep, ondone ) {
      var remote = this;
      remote.state = "downloading";
      od.download.schedule_download( remote, remote.added.concat( remote.changed ), onstep, function download_Cat_update_changed_done () {
         remote.state = "listed";
         remote.find_changed( ondone );
      });
   },

   /**
    * Get entry content
    * 
    * @param {type} id      Id of entry
    * @param {type} onload  Callback after successful load
    * @param {type} onerror Callback after error
    * @param {type} retry   Number of retry before throwing error.
    * @returns {undefined}
    */
   "get_data": function download_Cat_get_data ( id, onload, onerror, retry ) {
      var address = od.config.source.data( this.name, id );
      var remote = this;
      if ( retry === undefined ) retry = od.config.retry;
      _.cor( address,
         function download_Cat_get_data_callback ( data, xhr ){
            if ( ! data ) return downloar_Cat_get_data_retry( xhr, 'No Data' );
            _.call( onload, remote, remote, id, xhr.responseText );
         },
         downloar_Cat_get_data_retry
      );
      function downloar_Cat_get_data_retry ( xhr, err ) {
         _.warn( 'downloar_Cat_get_data_retry: ' + retry );
         --retry;
         if ( retry <= 0 ) {
            onerror ? _.call( onerror, remote, remote, id, err ) : _.error("Cannot download " + remote.name + " listing from " + address + ": " + err );
         } else remote.get_data( id, onload, onerror, retry );
      }
   },

   "reindex" : function download_Cat_reindex ( onstep, ondone, onerror ) {
      var remote = this, origial_state = remote.state;
      var local = od.data.get( remote.name );
      var idList, l, latch, count = 0;
      remote.state = 'downloading';

      function download_Cat_reindex_batch ( i ) {
         var l = i - 100;
         while ( i >= 0 && i > l ) {
            setImmediate( download_Cat_reindex_func( i, idList[ i ] ) );
            --i;
         }
         if ( i > 0 ) setTimeout( _.callfunc( download_Cat_reindex_batch, null, i ), 10 );
      }

      // Called once for each id. i is the position in index.
      function download_Cat_reindex_func ( i, id ) {
         return function download_Cat_reindex_task () {
            local.load_data( id, function download_Cat_reindex_loaded() {
               // Re-run local.update for reindex purpose
               local.update( id, local.raw[ i ], '<body>' + local.data[ id ] + '</body>', i );
               if ( remote.dirty.indexOf( id ) < 0 ) delete local.data[ id ]; // Unload to save memory if not dirty data
               download_Cat_reindex_step();
            }, function download_Cat_reindex_error () {
               // Delete item
               local.raw.splice( i, 1 );
               local.extended.splice( i, 1 );
               delete local.index[ id ];
               download_Cat_reindex_step();
            } );
         }
      }

      function download_Cat_reindex_step () {
         remote.progress = _.l( 'action.download.lbl_progress', null, ++count, l );
         _.call( onstep, remote, count, l );
         latch.count_down();
      }

      local.load_raw( function download_Cat_reindex_loaded () {
         idList = _.col( local.raw );
         l = idList.length;
         if ( l <= 0 ) {
            // If there is no data, do a delete instead.
            od.download.delete( remote );
            return _.call( ondone, remote );
         }
         local.ext_columns = local.parse_extended( local.raw_columns, null );
         local.extended = new Array( l );
         local.index = {};
         latch = new _.Latch( l );
         download_Cat_reindex_batch( l-1 );
         latch.ondone = function download_Cat_reindex_done ( ) { // Reindex tasks are wrapped in setImmediate
            remote.state = origial_state;
            remote.reindexed = true;
            local.count = local.raw.length;
            local.build_listing();
            if ( local.count > 0 ) {
               if ( remote.dirty.length <= 0 ) remote.dirty.push( idList[0] );
            } else {
               od.download.delete( remote );
            }
            _.call( ondone, remote );
         };
      }, onerror );
   },

   /**
    * Save this category's changed data.
    * 
    * @param {type} ondone  Callback after data is saved
    * @param {type} onerror Callback after error
    * @returns {undefined}
    */
   "save" : function download_Cat_save ( ondone, onerror ) {
      if ( this.dirty.length ) { // Has dirty
         var remote = this;
         var local = od.data.get( remote.name );
         local.save( function download_Cat_saved () {
            var latch = new _.Latch( remote.dirty.length+1 );
            remote.dirty.forEach( function download_Cat_save_data ( id ) {
               local.save_data( id, latch.count_down_function(), onerror );
            });
            latch.ondone = function download_Cat_saved_data () {
               remote.dirty = [];
               _.call( ondone, remote );
            };
            latch.count_down();
         }, onerror);
      } else {
         _.call( ondone, this );
      }
   }
};
_.seal( od.data.Category.prototype );