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
   /** Array of words to highlight */
   hl: null,
   /** RegExp pattern used in highlight */
   hlp : null,

   init : function gui_init () {
      _.l.detectLocale( 'en' );
      od.gui.l10n();
      od.gui.goto();
      // Perform navigation on pop state
      window.addEventListener( 'popstate', function window_popstate () {
         od.gui.goto();
      }, false);

      // Show / Hide 'top' buttons when scrolled
      function window_scroll () {
         var isTop = window.pageYOffset === 0;
         _.visible( '.btn_top', ! isTop );
         _.visible( '.btn_non_top', isTop );
      }
      window.addEventListener( 'scroll', window_scroll, false );
      window_scroll();

      // Monitor url change
      (function(){
         var gui = od.gui, get_act_id = gui.get_act_id;
         setInterval( function window_interval_url_monitor() {
            if ( get_act_id() !== gui.act_id ) od.gui.goto();
         }, od.config.url_monitor_interval );
      })();
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
      if ( ! action ) return gui.goto( 'list' );
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
      if ( act_id !== location.search.substr(1) ) {
         if ( history.pushState ) history.pushState( null, null, "?" + act_id );
      }
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

   update_title : function gui_update_title () {
      _('title')[0].textContent = _.l( 'gui.title', 'Offline 4e database - ' )
                                + _( "#action_" + od.gui.action.id + ' h1' )[0].textContent;
   },

   /**
    * Set highlight terms.
    * Terms will be processed for faster highlight processing.
    *
    * @param {Array} highlights Array of terms to highlight.
    */
   "set_highlight" : function gui_set_highlight( highlights ) {
      od.gui.hl = highlights;
      if ( ! highlights ) return;
      highlights = highlights.map( function ( e ) { return _.escRegx( _.escHtml( e ) ); } );
      // Join as alternatives and add a negative lookahead to prevent changing HTML tag.
      highlights = '(' + highlights.join( '|' ) + ')(?![^<]*>)';
      od.gui.hlp = new RegExp( highlights, 'ig' );
   },

   /**
    * Highlight text in given html using current highlight pattern.
    *
    * @param {String} html source html
    * @returns {String} result html
    */
   "highlight" : function gui_highlight( html ) {
      var hl = od.gui.hl;
      if ( ! hl ) return html; // Nothing to highlight (e.g. all exclude search)
      return html.replace( od.gui.hlp, '<span class="highlight">$1</span>' );
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