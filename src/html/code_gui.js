/*
 * code_gui.js
 * GUI-related codes, but activity-specific codes are coded with activities.
 */

// GUI namespace
od.gui = {
   /** Current action */
   action: null,
   initialized: [],

   /**
    * Switch between actions. Would call action object's cleanup method for current action and setup method for next action.
    * An action is {
    *   id         : "string id",
    *   initialize : function(){ triggered when this page is first used (before setup is called) },
    *   setup      : function(){ triggered when this page swap in },
    *   cleanup    : function(){ triggered when this page swap out, return false to abort switching },
    * }
    * 
    * @param {Object} action Action to switch to.
    */
   switch_action : function gui_switch_action( action ) {
      var gui = od.gui;
      var currentAction = gui.action;
      if ( !action || action === currentAction ) return;

      // Pre-switch validation & cleanup
      if ( currentAction ) {
         _.debug( "[Action] Cleanup " + currentAction.id );
         if ( _.call( currentAction.cleanup, currentAction, action ) === false ) return false;
      }

      // Hide other actions and show target page
      Array.prototype.forEach.call(_('body > div[id^="action_"]'), function(e){ e.style.display = ''; });
      _( "#"+action.id )[0].style.display = 'block';

      // Post-switch setup
      if ( gui.initialized.indexOf( action ) < 0 ) {
         _.info( "[Action] Initialize " + action.id );
         gui.initialized.push( action );
         _.call( action.initialize, action );
      }
      _.info( "[Action] Setup " + action.id );
      _.call( action.setup, action, currentAction );

      gui.action = action;
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
   }

};