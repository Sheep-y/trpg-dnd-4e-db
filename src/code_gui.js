<script type='text/javascript'>'use strict';
/*
 * code_gui.js
 * GUI-related codes, but activity-specific codes are coded with activities.
 */

// GUI namespace
oddi.gui = {
   /** Current action */
   action: null,
   initialized: [],

   /**
    * Switch between actions. Would call action object's cleanup method for current action and setup method for next action.
    */
   switch_action : function gui_switch_action( action ) {
      var gui = oddi.gui;
      var currentAction = gui.action;
      if ( !action || action == currentAction ) return;

      // Pre-switch validation & cleanup
      if ( currentAction && currentAction.cleanup && currentAction.cleanup( action ) === false ) return false;

      Array.prototype.forEach.call(_("body > div[id]"), function(e){ e.style.display = ''; });
      _( "#"+action.id )[0].style.display = 'block';

      // Post-switch setup
      if ( gui.initialized.indexOf( action ) < 0 ) {
         gui.initialized.push( action );
         if ( action.initialize != null ) action.initialize();
      }
      if ( action.setup ) action.setup( action );

      gui.action = action;
   },

   /** Handle ajax errors by showing message */
   ajax_error : function gui_ajax_error( address, onfail ) {
      return function gui_ajax_error_handler( xhr ) {
         var msg = _.l( 'error.ajax_error', xhr.statusText, address );
         if ( onfail ) onfail( msg ); else alert( msg );
       };
   },

   /** Show and set message / hide if message is empty */
   set : function gui_set( id, message ) {
      if ( !message ) {
         // Hide element
         Array.prototype.forEach.call( _(id), function(e){
            e.style.display = 'none';
         } );

      } else {
         // Set message and shows
         Array.prototype.forEach.call( _(id), function(e){
            e.innerHTML = message;
            e.style.display = '';
         } );
      }
   },


};

</script>