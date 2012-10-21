<script type='text/javascript'>'use strict';
/*
 * data_writer.js
 * Write data model to persistent storage.
 * This part needs file permission and currently only works with Firefox
 */

oddi.writer = {
   _fs : null,

   _write : function writer_write( file, content ) {
      if ( window.ActiveXObject ) {
         // IE ActiveX writer. Write file in UTF-16 and windows linebreak, not much choice here.
         //file = oddi.config.data_full_path + file;
         try {
            var fso = oddi.writer._fs;
            if ( oddi.writer._fs === null ) {
               alert( _.l( 'error.file_grant_permission' ) );
               fso = oddi.writer._fs = new ActiveXObject("Scripting.FileSystemObject");
            }
            folder = file.replace( /[\/\\][^\/\\:*?"<>|]+$/, '' );
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
            _.log( 'Write to '+file );
            var file = fso.OpenTextFile( file, 2, true, -1); // = OpenTextFile( file, WRITE, CREATE, UNICODE )
            content.split( '\n' ).forEach( function(line){ file.WriteLine( line ); } );
            file.Close();
            _.log( 'Done' );
         } catch (e) {
            if (e.number == -2146827859) alert('Unable to access local files due to browser security settings. Go to Tools->Internet Options->Security->Custom Level. Enable "Initialize and script ActiveX controls not marked as safe".');
            else alert(e);
         }
      } else {
         alert( _.l( 'error.file_no_api' ) );
         return false;
      }
   },

   _delete : function writer_write( file ) {
      if ( window.ActiveXObject ) {
         // IE ActiveX file system.
         file = oddi.writer.basePath + file;
         try {
            var fso = oddi.writer._fs;
            if ( oddi.writer._fs === null ) {
               alert( _.l( 'error.file_grant_permission' ) );
               fso = oddi.writer._fs = new ActiveXObject("Scripting.FileSystemObject");
            }
            if ( fso.FileExists( file ) ) {
               _.log( 'Delete '+file );
               fso.DeleteFile( file );
               if ( fso.FileExists( file ) ) {
                  _.error( _.l( 'error.file_cannot_delete', file ) );
                  return false;
               }
            }
         } catch (e) {
            if (e.number == -2146827859) alert( _.l('error.com_file_security'));
            else alert(e);
         }
      } else {
         alert( _.l( 'error.file_no_api' ) );
         return false;
      }
   },

   // Writing is instantaneous, but let's use callback for future compatibility
   write_index : function writer_write_category( onload ) {
     // oddi.reader.jsonp_index( 20120915, {
      var object = {}, data = oddi.data.category;
      for ( var cat in data ) object[cat] = data[cat].count();
      this._write( oddi.file.index(),
         'oddi.reader.jsonp_index( 20120915,' + JSON.stringify( object ) + ')' );
      if ( onload ) onload();
   },

   write_data_listing : function writer_write_data_listing( category, onload ) {
     // oddi.reader.jsonp_data_listing( 20120915, "Sample", [ "Id", "Name", "Category", "SourceBook" ], [
      this._write( oddi.file.category_listing( category.name ),
         'oddi.reader.jsonp_data_listing( 20120915, "'+_.escJs(category.name)+'",'
         +JSON.stringify( category.columns ) + ','
         +JSON.stringify( category.listing ) + ')' );
      if ( onload ) onload();
   },

   write_data_index : function writer_write_data_index( category, onload ) {
      this._write( oddi.file.category_index( category.name ),
       'oddi.reader.jsonp_data_index( 20121020, "'+_.escJs(category.name)+'",'
       +JSON.stringify( category.index ) + ')' );
      if ( onload ) onload();
   },

   write_data : function writer_write_data( category, from, to, onload ) {
      this._write( oddi.file.category_index( category.name ),
       'oddi.reader.jsonp_data(20120915, "'+_.escJs(category.name)+'",'+from+','+
       JSON.stringify( category.data.slice( from, to ) ) + ')' );
      if ( onload ) onload();
   },

};

</script>