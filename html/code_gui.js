/*
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

   /** True to show highlight, false to not show */
   hl_enabled : true,
   /** RegExp pattern used in highlight */
   hlp : null,

   /** Max. row per page */
   row_per_page: 200,
   /** Current list page. Zero based. */
   page: 0,
   /** Total page count. */
   total_page: 0,

   init : function gui_init () {
      _( 'link[rel="icon"]' )[0].href = od.config.data_read_path + "/res/icon.png";
      _.l.detectLocale( 'en' );
      od.gui.l10n();
      od.gui.go();
      // Perform navigation on pop state
      _.attr( window, {
         'onpopstate' : function window_popstate () {
            od.gui.go();
         },
         'onkeypress' : function window_keypress ( evt ) {
            if ( evt.altKey || evt.ctrlKey || evt.metaKey || evt.shiftKey ) return;
            if ( document.activeElement && document.activeElement.tagName === 'INPUT' ) return;
            switch ( evt.key ) {
               case "ArrowLeft":
                  var left = _( 'section[id^=action_][style*=block] > nav > .btn_prev:not([style*=none])' )[0];
                  if ( left ) left.click();
                  break;
               case "ArrowRight":
                  var right = _( 'section[id^=action_][style*=block] > nav > .btn_next:not([style*=none])' )[0];
                  if ( right ) right.click();
                  break;
               case "Escape":
                  if ( od.gui.get_act_id().startsWith( 'view' ) )
                     od.gui.action.btn_browse_click();
                  break;
            }
         }
      } );

      // Monitor url change - disabled because Chrome no longer support history push/replace
      /*
      (function(){
         var gui = od.gui, get_act_id = gui.get_act_id;
         setInterval( function window_interval_url_monitor() {
            if ( get_act_id() !== gui.act_id ) gui.go();
         }, od.config.url_monitor_interval );
      })();
      */
   },

   /**
    * Localise current action.
    */
   l10n : function gui_l10n () {
      _.l.localise();
      if ( od.gui.action ) _.call( od.gui.action.l10n );
   },

   /**
    * Navigate to given activity page.
    *
    * @param {String} act_id Action to switch to, with all necessary parameters
    */
   "go" : function gui_go ( act_id ) {
      var gui = od.gui;
      if ( act_id === undefined ) act_id = gui.get_act_id();
      var action = od.action[act_id];
      _.info( "[Action] Navigate to " + act_id );
      // If not a simple page like help/license, try to find the action to handle this url
      if ( ! action ) {
         var firstword = _.ary( act_id.match( /^\w+/ ) )[ 0 ]; // Parse first word in url
         for ( var aid in od.action ) {
            var act = od.action[ aid ];
            // Either firstword is an exact match on id, or match action's url pattern
            if ( firstword === aid || ( act.url && act.url.test( act_id ) ) ) {
               action = act;
               if ( ! action.id ) action.id = aid;
               break;
            }
         }
      } else {
         if ( ! action.id ) action.id = act_id;
      }
      // Fallback to 'list' if failed.
      if ( ! action ) {
         if ( act_id !== 'list' ) return gui.go( 'list' );
         gui.act_id = 'list'; // Prevent triggering window_interval_url_monitor
         return;
      }
      // Update url and swap page.
      gui.pushState( act_id );
      gui.switch_action( action );
   },

   /**
    * Call history.pushState
    *
    * @param {String} act_id location.search part (without '?')
    */
   "pushState" : function gui_push_state ( act_id ) {
      if ( act_id !== location.search.substr(1) ) try {
         if ( history.pushState ) history.pushState( null, null, "?" + act_id );
      } catch ( err ) { /* https://bugs.chromium.org/p/chromium/issues/detail?id=301210 */ }
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
   "switch_action" : function gui_switch_action ( action ) {
      var gui = od.gui;
      var currentAction = gui.action;
      if ( ! action ) return _.warn( '[Action] Null or undefined action.' );
      if ( action === currentAction ) {
         _.info( "[Action] Already at " + action.id );
         _.call( action.setup, action, gui.act_id );
         return;
      }

      _.time();
      // Pre-switch validation & cleanup
      if ( currentAction ) {
         _.time( "[Action] Cleanup " + currentAction.id );
         if ( _.call( currentAction.cleanup, currentAction, action ) === false ) return false;
      }

      // Hide other actions and show target page
      _.style('body > section[id^="action_"]', 'display', '' );
      var page = _( "#action_" + action.id )[0];
      page.style.display = 'block';

      // Post-switch setup
      if ( gui.initialized.indexOf( action ) < 0 ) {
         _.time( "[Action] Initialize " + action.id );
         gui.initialized.push( action );
         _.call( action.initialize, action );
      }
      _.time( "[Action] Setup & l10n " + action.id );
      _.call( action.setup, action );
      _.call( action.l10n, action );
      gui.action = action;

      gui.update_title();
      _.time( '[Action] Switched to ' + action.id );
   },

   update_title : function gui_update_title ( title ) {
      if ( title )
         _('title')[0].textContent = title;
      else
         _('title')[0].textContent = od.gui.action
            ? _( "#action_" + od.gui.action.id + ' h1' )[0].textContent
            : _.l( 'gui.title', '4e Database' );
   },

   /**
    * Set highlight terms.
    * Terms will be processed for faster highlight processing.
    *
    * @param {Array} highlights Array of terms to highlight.
    */
   "set_highlight" : function gui_set_highlight ( highlights ) {
      var gui = od.gui;
      if ( ! highlights ) return gui.hlp = null;
      // Join as alternatives and add a negative lookahead to prevent changing HTML tag.
      highlights = '((?:' + highlights.join( ')|(?:' ) + '))(?![^<]*>)';
      gui.hlp = new RegExp( highlights, 'ig' );
   },

   /**
    * Highlight text in given html using current highlight pattern.
    *
    * @param {String} html source html
    * @returns {String} result html
    */
   "highlight" : function gui_highlight ( html, hl ) {
      if ( ! hl ) hl = od.gui.hlp;
      if ( ! hl ) return html; // Nothing to highlight (e.g. all exclude search)
      // Apply highlight, then concat space separated highlights
      return html.replace( hl, '<mark>$1</mark>' ).replace( /<\/mark>(\s+)<mark>/g, '$1' );
   },

   /**
    * Enable / disable keywords highlight.
    */
   'toggle_highlight' : function gui_toggle_highlight ( state ) {
      var gui = od.gui;
      if ( state === undefined )
         gui.hl_enabled = ! gui.hl_enabled;
      else
         gui.hl_enabled = state;
      _.info( "[Config] Toggle highlight " + ( gui.hl_enabled ? 'on' : 'off' ) );
      document.body.classList[ gui.hl_enabled ? 'remove' : 'add' ]( 'no_highlight' );
      _.prop( '#action_about_rdo_highlight_' + ( od.gui.hl_enabled ? 'on' : 'off' ), { 'checked': true } );
   },
};