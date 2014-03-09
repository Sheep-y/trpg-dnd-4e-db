/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * code_gui.js
 *
 * GUI-related codes, but activity-specific codes are coded with activities.
 */

// GUI namespace
od.gui = {
   /** Current action id. */
   act_id: null,
   /** Current action. */
   action: null,
   /** List of initiated actions. */
   initialized: [], 

   init : function gui_init ( ) {
      _.l.detectLocale( 'en' );
      _.l.localise();
      od.gui.goto( );
      // Perform navigation on pop state
      window.addEventListener( 'popstate', function window_popstate () {
         od.gui.goto();
      }, false);

      // Monitor url change
      (function(){
         var gui = od.gui, get_act_id = gui.get_act_id;
         setInterval( function window_interval_url_monitor() {
            if ( get_act_id() != gui.act_id ) od.gui.goto();
         }, od.config.url_monitor_interval );
      })();
   },

   /**
    * Navigate to given activity page.
    *
    * @param {String} act_id Action to switch to, with all necessary parameters
    */
   "goto" : function gui_goto ( act_id ) {
      var gui = od.gui;
      if ( act_id === undefined ) act_id = gui.get_act_id();
      var action = od.action[act_id], id = act_id;
      _.time( "[Action] Navigate to " + act_id );
      // If not a simple page like about/download etc., try to find the action to handle this url
      if ( ! action ) {
         var firstword = _.ary( act_id.match( /^\w+/ ) )[ 0 ]; // Parse first word in url
         for ( id in od.action ) {
            action = od.action[ id ];
            // Either firstword is an exact match on id, or match action's url pattern
            if ( firstword === id || ( action.url && action.url.test( act_id ) ) ) break;
         }
      }
      // Set id if absent. Then update url and swap page.
      if ( action.id === undefined ) action.id = id;
      gui.pushState( act_id );
      gui.switch_action( action );
   },
   
   /**
    * Call history.pushState
    *
    * @param {String} act_id location.search part (without '?')
    */
   "pushState" : function gui_push_state ( act_id ) {
      if ( act_id !== location.search.substr(1) ) 
         if ( history.pushState ) history.pushState( null, null, "?" + act_id );
      od.gui.act_id = act_id;
   },

   "get_act_id" : function gui_get_act_id () {
      return location.search ? location.search.substr(1) : "list";
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
      if ( !action || action === currentAction ) {
         if ( action.navigate ) action.navigate( od.gui.act_id );
         return;
      }

      _.time();
      // Pre-switch validation & cleanup
      if ( currentAction ) {
         _.time( "[Action] Cleanup " + currentAction.id );
         if ( _.call( currentAction.cleanup, currentAction, action ) === false ) return false;
      }

      // Hide other actions and show target page
      Array.prototype.forEach.call(_('body > div[id^="action_"]'), function(e){ e.style.display = ''; });
      var page = _( "#action_" + action.id )[0];
      page.style.display = 'block';

      // Post-switch setup
      if ( gui.initialized.indexOf( action ) < 0 ) {
         _.time( "[Action] Initialize " + action.id );
         gui.initialized.push( action );
         _.call( action.initialize, action );
      }
      _.time( "[Action] Setup " + action.id );
      _.call( action.setup, action, currentAction );
      setImmediate( function gui_switch_action_immediate () {
         _('title')[0].textContent = od.config.title_prefix + _( page, 'h1' )[0].textContent;
      } );

      gui.action = action;
      _.time( 'Switched to ' + action.id );
   },

   /**
    * Convert special unicode symbols to common symbols for safe display.
    *
    * @param {String} str Input string.
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