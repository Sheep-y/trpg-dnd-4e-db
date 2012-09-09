<script>'use strict';
/*
 * code_gui.js
 * GUI-related codes, but activity-specific codes are coded with activities.
 */
 
// Switch to download action on domready
window.addEventListener('DOMContentLoaded',function(){ oddi.gui.switch_action( oddi.action.download ); },false);

// Action object. Would be populated by individual actions
oddi.action = {};

// 
oddi.gui = {
   /** Current action */
   action: null,
   
   switch_action :
      /**
       * Switch between actions. Would call action object's cleanup method for current action and setup method for next action.
       */
      function gui_switch_action( action ) {
         var currentAction = oddi.gui.action;
         if ( !action || action == currentAction ) return;

         // Pre-switch validation & cleanup
         if ( currentAction && currentAction.cleanup && currentAction.cleanup( action ) === false ) return false;

         Array.prototype.forEach.call(_("body > div[id]"), function(e){ e.style.display = ''; });
         _( "#"+action.id )[0].style.display = 'block';

         // Post-switch setup
         if ( action.setup ) action.setup( action );

         oddi.gui.action = action;
      }
}

</script>