<script type='text/javascript'>'use strict';
/*
 * data_model.js
 * Create data model in memory
 */

oddi.data = {
   category : { "Sample": 2 },
   data : {
      "Sample" : {
         columns: [ "ID", "Name", "Category", "SourceBook" ],
         listing: [ [ "sampleId001", "Sample Data", "Sample1", "Git" ],
                    [ "sampleId003", "Sample Data 3", "Sample3", "Git" ], ],
         index: {"this":[0,1],"sample":[0],"data":[0,1],"sampleId001":[0],"another":[1],"that":[1],"example":[1]},
         data: [ "<p>This is sample data id sampleId001</p>" ], // Data at index 2 not loaded yet
      }
   },

   find_in_list : function data_find_in_list( list, id ) {
      for ( var i = 0, len = list.length ; i < len ; i++ )
         if ( list[i][0] === id )
            return list[i];
      return null;
   },

   /** Clear all loaded data or a category of data */
   clear: function data_reset( category ) {
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
   }
}


/*
oddi.data = {
   data: { "cat": {
      "count": entry_count,
   },
}
*/

</script>