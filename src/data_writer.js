<script type='text/javascript'>'use strict';
/*
 * data_writer.js
 * Write data model to persistent storage.
 * This part needs file permission and currently only works with Firefox
 */

oddi.writer = {
   _fs : null,

   _call : function writer_call ( com_callback ) {
      if ( window.ActiveXObject ) {
         if ( com_callback ) {
            try {
               var fso = oddi.writer._fs;
               if ( fso === null ) {
                  alert( _.l( 'error.file_grant_permission' ) );
                  fso = oddi.writer._fs = new ActiveXObject("Scripting.FileSystemObject");
               }
               com_callback( fso );
            } catch (e) {
               if (e.number == -2146827859) alert( _.l( 'error.com_file_security') );
               else _.error(e);
            }
         }
      } else {
         alert( _.l( 'error.file_no_api' ) );
      }
   },

   _write : function writer_write( file, content, ondone ) {
      // IE ActiveX writer. Write file in UTF-16 and windows linebreak, not much choice here.
      //file = oddi.config.data_full_path + '/' + file;
      this._call( function writer_com_write( fso ) {
         _.log( 'Write to '+file );
         // Create root data folder if not exists
         if ( ! fso.FolderExists( oddi.config.data_full_path ) ) {
            fso.CreateFolder( oddi.config.data_full_path );
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
         var f = fso.OpenTextFile( file, 2, true, -1); // = OpenTextFile( file, WRITE, CREATE, UNICODE )
         content.split( '\n' ).forEach( function(line){ f.WriteLine( line ); } );
         f.Close();
         if ( ondone ) ondone();
      }, null );
   },

   _delete : function writer_write( file, ondone ) {
      this._call( function writer_com_delete( fso ) {
         // IE ActiveX file system.
         file = oddi.writer.basePath + file;
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
   write_index : function writer_write_category( ondone ) {
     // oddi.reader.jsonp_index( 20120915, {
      var object = {}, data = oddi.data.category;
      for ( var cat in data ) object[cat] = data[cat].count();
      this._write( oddi.file.index(),
         'oddi.reader.jsonp_index(20120915,' + JSON.stringify( object ) + ')', ondone );
   },

   write_data_listing : function writer_write_data_listing( category, ondone ) {
     // oddi.reader.jsonp_data_listing( 20120915, "Sample", [ "Id", "Name", "Category", "SourceBook" ], [
      this._write( oddi.file.category_listing( category.name ),
         'oddi.reader.jsonp_data_listing(20120915,"'+_.escJs(category.name)+'",'
         +JSON.stringify( category.columns ) + ','
         +JSON.stringify( category.listing ) + ')', ondone );
   },

   write_data_index : function writer_write_data_index( category, ondone ) {
      this._write( oddi.file.category_index( category.name ),
       'oddi.reader.jsonp_data_index(20121020,"'+_.escJs(category.name)+'",'
       +JSON.stringify( category.index ) + ')', ondone );
   },

   write_data : function writer_write_data( category, from, to, ondone ) {
      this._write( oddi.file.data( category.name, from ),
       'oddi.reader.jsonp_data(20120915,"'+_.escJs(category.name)+'",'+from+','+
       JSON.stringify( category.data.slice( from, to ) ) + ')', ondone );
   },

};

</script>