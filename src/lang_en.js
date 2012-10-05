<script type='text/javascript'>'use strict';
/*
 * lang_en.js
 * English localization resources
 */

_.l.setLocale( 'en' );

_.l.set( 'error', {
   // 'no_cross_origin' : 'Cross Origin' // TODO: implement proper cross origin alert
   'ajax_error' : 'Cannot load content (%1) from %2.',
});

_.l.set( 'action.download', {
   title: "Offline Compendium",
   paragraph: [
      "This browser script allows you to download and save compendium entries, then search and list them offline using advanced Google-like syntax.",
      "<b>Requires an active DDI subscription to download any data.</b> <a href='http://www.wizards.com/DnD/Article.aspx?x=dnd/updates' target='_blank'>Official Rules Updates</a>"
   ],
   btn_refresh: "Refresh list",
   chk_refresh: "Auto refresh",
   btn_update_changed: "Update %1 new and %2 changed",
   btn_update_all: "Update all %1",
   btn_clear_existing: "Clear Data",
   lbl_new_items: "New Entries",
   lbl_changed_items: "Changed Entries",
   lbl_deleted_items: "Removed Entries",
   msg_clearExisting: "Delete existing data?",
});

</script>