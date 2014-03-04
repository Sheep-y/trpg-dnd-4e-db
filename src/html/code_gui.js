/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * code_gui.js
 *
 * GUI-related codes, but activity-specific codes are coded with activities.
 */

// GUI namespace
od.gui = {
   /** Current action */
   action: null,
   initialized: [],

   "goto" : function gui_goto ( url ) {
      var action = od.action[url], id = url;
      _.debug( "[Action] Navigate to " + url );
      // If not simple page, try to find the action to handle this url
      if ( ! action ) {
         var firstword = _.ary( url.match( /^\w+/ ) )[ 0 ]; // Parse first word in url
         for ( id in od.action ) {
            action = od.action[ id ];
            // Either firstword is an exact match on id, or match action's url pattern
            if ( firstword === id || ( action.url && action.url.test( url ) ) ) break;
         }
      }
      // Set id if absent. Then update url and swap page.
      if ( action.id === undefined ) action.id = id;
      if ( history.pushState && url !== location.search.substr(1) ) history.pushState( null, null, "?" + url );
      this.switch_action( action );
   },

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
   "switch_action" : function gui_switch_action( action ) {
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
      var page = _( "#action_" + action.id )[0];
      page.style.display = 'block';

      // Post-switch setup
      if ( gui.initialized.indexOf( action ) < 0 ) {
         _.info( "[Action] Initialize " + action.id );
         gui.initialized.push( action );
         _.call( action.initialize, action );
      }
      _.info( "[Action] Setup " + action.id );
      _.call( action.setup, action, currentAction );
      setImmediate( function gui_switch_action_immediate () {
         _('title')[0].textContent = od.config.title_prefix + _( page, 'h1' )[0].textContent;
      } );

      gui.action = action;
   },

   /**
    * Convert special unicode symbols to common symbols for safe display.
    *
    * @param {String} Input string.
    * @returns {String} Safe version of input converted to common symbols
    */
   "symbol_safe_convert" : function gui_symbol_safe_convert ( str ) {
      if ( od.config.symbol_conversion === false ) return str;
      var mapping = od.config.symbols[od.config.symbol_conversion];
      if ( mapping === undefined ) return str;
      return str.replace( /͜͡[⋖⚔➶✻]|[✦⋖⚔➶✻⚀⚁⚂⚃⚄⚅☼]/g, function gui_symbol_safe_convert_replace( txt ) {
         return txt in mapping ? mapping[txt] : txt;
      });
   }

};