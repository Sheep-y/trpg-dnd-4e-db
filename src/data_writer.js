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
         file = oddi.cofnig.data_full_path + file;
         try {
            var fso = oddi.writer._fs;
            if ( oddi.writer._fs === null ) {
               alert( _.l( 'error.file_grant_permission' ) );
               fso = oddi.writer._fs = new ActiveXObject("Scripting.FileSystemObject");
            }
            /*// No need to delete, file will be overwritten
            if ( fso.FileExists( file ) ) {
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
            if (e.number == -2146827859) alert('Unable to access local files due to browser security settings. Go to Tools->Internet Options->Security->Custom Level. Enable "Initialize and script ActiveX controls not marked as safe".');
            else alert(e);
         }
      } else {
         alert( _.l( 'error.file_no_api' ) );
         return false;
      }
   }
};

</script>