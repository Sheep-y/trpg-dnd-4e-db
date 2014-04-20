/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * data_writer.js
 *
 * Write data model to local files.
 * This part needs file permission and currently only works with Internet Explorer (non-metro)
 */

od.writer = {
   _fs : null, // File system object cache
   _state: 'initial', // 'initial', 'failed', 'success'

   _call : function writer_call ( com_callback, onerror ) {
      if ( _.is.activeX() ) { // IE 11 reports no window.ActiveX, but can still create them.
         if ( com_callback ) {
            try {
               var fso = od.writer._fs;
               if ( fso === null ) {
                  if ( od.writer._state === 'initial' ) {
                     alert( _.l( 'error.file_grant_permission' ) );
                     od.writer._state = 'failed';
                  }
                  fso = od.writer._fs = new ActiveXObject("Scripting.FileSystemObject");
                  od.writer._state = 'success';
               }
               com_callback( fso );
            } catch (e) {
               if (e.number === -2146827859) e = _.l( 'error.com_file_security');
               if ( onerror === undefined ) onerror = _.error;
               _.call( onerror, null, e );
            }
         }
      } else {
         alert( _.l( 'error.file_no_api' ) );
      }
   },

   _write : function writer_write ( file, content, ondone, onerror ) {
      // IE ActiveX writer. Write file in UTF-16 and windows linebreak, not much choice here.
      //file = od.config.data_full_path + '/' + file;
      this._call( function writer_com_write( fso ) {
         _.log( 'Write to '+file );
         // Create root data folder if not exists
         if ( ! fso.FolderExists( od.config.data_write_path ) ) {
            fso.CreateFolder( od.config.data_write_path );
         }
         // Create containing folder if not exists
         var folder = file.replace( /[\/\\][^\/\\:*?"<>|]+$/, '' );
         if ( ! fso.FolderExists( folder ) ) {
            fso.CreateFolder( folder );
         }
         /*// No need to delete, file will be overwritten
         else if ( fso.FileExists( file ) ) {
            _.log( 'Delete '+file );
            fso.DeleteFile( file );
            if ( fso.FileExists( file ) ) {
               _.error( _.l( 'error.file_cannot_delete', file ) );
               return false;
            }
         }*/
         var f = fso.CreateTextFile ( file, true, true ); // true = overwrite, true = unicode
         content.split( '\n' ).forEach( function(line){ f.WriteLine( line ); } );
         //f.BinaryWrite( new ActiveXObject("Utf8Lib.Utf8Enc").UnicodeToUtf8(content) ); // No such method: BinaryWrite
         f.Close();
         if ( ondone ) ondone();
      }, onerror );
   },

   _delete : function writer_write ( file, ondone ) {
      this._call( function writer_com_delete( fso ) {
         // IE ActiveX file system.
         file = od.writer.basePath + file;
         if ( fso.FileExists( file ) ) {
            _.log( 'Delete '+file );
            fso.DeleteFile( file );
            if ( fso.FileExists( file ) ) {
               return _.error( _.l( 'error.file_cannot_delete', file ) );
            }
         }
         if ( ondone ) ondone();
      }, null );
   },

   // Writing is instantaneous, but let's use callback for future compatibility
   write_catalog : function writer_write_catalog ( ondone, onerror ) {
      var object = {}, data = od.data.category;
      for ( var cat in data ) object[cat] = data[cat].count;
      this._write( od.config.file.catalog(), 'od.reader.jsonp_catalog(20130703,' + JSON.stringify( object ) + ')', ondone, onerror );
   },

   write_data_raw : function writer_write_data_raw ( category, ondone, onerror ) {
      this._write( od.config.file.raw( category.name ),
         'od.reader.jsonp_data_raw(20130703,"' + _.escJs( category.name ) + '",'
         + JSON.stringify( category.raw_columns ) + ','
         + JSON.stringify( category.raw ) + ')', ondone, onerror );
   },

   write_data_listing : function writer_write_data_listing ( category, ondone, onerror ) {
      var str = JSON.stringify( category.extended );
      if ( od.config.data_write_compress ) {
         str = 'JSON.parse(LZString.decompress("' + LZString.compress( str ) + '"))';
      }
      this._write( od.config.file.listing( category.name ),
         'od.reader.jsonp_data_listing(20140414,"' + _.escJs( category.name ) + '",'
         + JSON.stringify( category.ext_columns ) + ','
         + str + ')', ondone, onerror );
   },

   write_data_index : function writer_write_data_index ( category, ondone, onerror ) {
      var str = JSON.stringify( category.index );
      if ( od.config.data_write_compress ) {
         str = 'JSON.parse(LZString.decompress("' + LZString.compress( str ) + '"))';
      }
      this._write( od.config.file.index( category.name ),
       'od.reader.jsonp_data_index(20140414,"' + _.escJs( category.name ) + '",' + str + ')', ondone );
   },

   write_data : function writer_write_data ( category, id, data, ondone, onerror ) {
      this._write( od.config.file.data( category.name, id ),
       'od.reader.jsonp_data(20130703,"' + _.escJs( category.name ) + '","' + _.escJs( id )+'",'+
       JSON.stringify( data ) + ')', ondone );
   }

};