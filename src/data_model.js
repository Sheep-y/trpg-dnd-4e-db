<script type='text/javascript'>'use strict';
/*
 * data_model.js
 * Create data model in memory
 */

oddi.data = {
   category : { "Sample": 2 },
   data : {
      "Sample" : {
         "columns": [ "ID", "Name", "Category", "SourceBook" ],
         "listing": [ [ "sampleId001", "Sample Data", "Sample1", "Git" ],
                    [ "sampleId003", "Sample Data 3", "Sample3", "Git" ], ],
         "index": [ "This is sample data id sampleId001", "This is another data that is an example." ],
         "data": [ "<p>This is sample data id sampleId001</p>" ], // Data at index 2 not loaded yet
      }
   },

   find_in_list : function data_find_in_list( list, id ) {
      for ( var i = 0, len = list.length ; i < len ; i++ )
         if ( list[i][0] === id )
            return i;
      return -1;
   },

   /** Clear all loaded data or a category of data */
   clear : function data_reset( category ) {
      if ( category ) {
         var pos = this.category.indexOf( category );
         if ( pos ) {
            this.category.splice( pos, 1 );
            delete this.data[category];
         } else {
            if ( console ) console.log("Warning: Cannot reset category "+category);
         }
      } else {
         this.category = [];
         this.data = {};
      }
   },

   create_category : function data_creaete_category( category ) {
      return oddi.data.data[category] = {
         "columns" : [],
         "listing" : [],
         "index" : [],
         "data" : [],
      }
   },

   set_columns : function data_set_columns( category, columns ) {
      var cat = oddi.data.create_category( category );
      oddi.data.data[category].columns = columns;
      return cat;
   },

   /** Insert or update an entry */
   update : function data_update( category, id, columns, listing, content ) {
      if ( content ) {
         var data = oddi.data;
         var cat = data.data[category];
         var i;
         if ( cat === undefined ) {
            // New category
            cat = data.set_columns( category, columns );
            i = -1;
         } else {
            // Existing category, check columns
            if ( cat.columns.toString() !== columns.toString() ) {
               alert("Columns mismatch for "+listing[1]+" in "+category);
               data.set_columns( columns );
            }
            var i = data.find_in_list( cat.listing, id );
         }
         if ( i >= 0 ) {
            data.remove_index( cat, i );
            cat.listing[i] = listing;
            cat.data[i] = content;
            data.update_index( cat, i );
         } else {
            i = cat.listing.push( listing )-1;
            cat.data.push( content );
         }
         data.update_index( cat, i );
      } else {
         if ( window.console && console.warn ) console.warn( timeToStr() + " No data or cannot parse data for "+category+"."+id );
      }
   },

   /**
    * Pre-process data - extract content, remove scripts and forms, normalise symbols and links etc.
    */
   preprocess : function data_preprocess( data ) {
      // Normalise input
      data = data.trim().replace( /\r\n?/g, '\n' );
      // Extract body
      data = data.match( /<body[^>]*>((?:.|\n)*)<\/body\s*>/ );
      if ( !data ) return null;
      // Normalise whitespace
      data = data[1].trim().replace( /[\n\s]+/g, ' ' );
      // Remove script and form tags
      data = data.trim().replace( /<script\b.*?<\/script\s*>/g, '' ).replace( /<input[^>]*>/g, '').replace( /<form[^>]*>|<\/form\s*>/g, '' );
      // Remove empty contents
      data = data.replace( /<div[^>]*>\s*<\/div\s*>/g, '' );
      // TODO: convert ' and links
      return data;
   },

   /** Remove the index of an existing entry. For internal use. */
   remove_index : function data_remove_index( cat, index ) {
      // Currently unused
   },

   /** Update the index of an existing entry. For internal use. */
   update_index : function data_update_index( cat, index ) {
      cat.index[index] = cat.data[index].replace( /<[^>]+>/g, '' ).replace( /\s+/g, ' ' );
   },
};

</script>