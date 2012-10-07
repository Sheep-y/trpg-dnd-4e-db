<script type='text/javascript'>'use strict';
/*
 * lang_en.js
 * English localization resources
 */

_.l.setLocale( 'en' );

_.l.set( 'error', {
   // 'no_cross_origin' : 'Cross Origin' // TODO: implement proper cross origin alert
   'ajax_error' : 'Cannot load content (%1) from %2.',

   'file_grant_permission' : 'You may see a prompt. Please grant us the permission update data files.',
   'file_no_api' : 'Only support saving in IE, because JavaScript has no standard for file writing.',
   'file_cannot_delete' : 'Cannot overwrite %1',
});

_.l.set( 'action.download', {
   title : "Update Offline Compendium",
   paragraph : [
      "This browser script allows you to download and save compendium entries, then search and list them offline using advanced Google-like syntax.",
      "<b>Requires an active DDI subscription to download any data.</b> <a href='http://www.wizards.com/DnD/Article.aspx?x=dnd/updates' target='_blank'>Official Rules Updates</a>"
   ],
   btn_refresh : "Refresh list",
   chk_refresh : "Auto refresh",
   btn_clear_existing : "Clear Data",
   btn_update_changed : "Update %1 new and %2 changed",
   btn_update_all : "Update all %1",
   btn_done       : "Browse Data",
   lbl_new_items  : "New Entries",
   lbl_changed_items : "Changed Entries",
   lbl_deleted_items : "Removed Entries",
   msg_clearExisting : "Delete existing data?",
});

_.l.set( 'action.list', {
   title : "Serch Offline Compendium",
   btn_update : "Update Compendium",
});

</script>