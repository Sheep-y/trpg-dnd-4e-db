/*
 * lang_en.js
 * English localization resources
 */

_.l.setLocale( 'en' );


_.l.set( 'data', {
   'category' : {
      'EpicDestiny' : 'Epic Destiny',
      'ParagonPath' : 'Paragon Path',
   },
   'field' : {
      "PowerSourceText" : 'Power Source',
      "RoleName" : 'Role',
      "KeyAbilities" : 'Abilities',
      "GroupRole" : 'Group',
      "DescriptionAttribute" : 'Attribute',
      "ComponentCost" : 'Component',
      "KeySkillDescription" : 'Key Skill',
      'ClassName': 'Class',
      'SourceBook': 'Source'
   }
});

_.l.set( 'error', {
   'updating_data' : 'Error when updating %1 of %2 (%3)',

   // 'no_cross_origin' : 'Cross Origin' // TODO: implement proper cross origin alert
   'ajax_error' : 'Cannot load content (%1) from %2',

   'com_file_security' : 'Cannot save data due to browser security settings. Go to Tools > Internet Options > Security > Custom Level. Enable "Initialize and script ActiveX controls not marked as safe".',
   'file_grant_permission' : 'You may see a prompt. Please grant us the permission update data files.',
   'file_no_api' : 'Only support saving in IE, because JavaScript has no standard for saving portable offline data.',
   'file_cannot_delete' : 'Cannot overwrite %1',

   'wrong_ext': 'Extended list different from raw list, please re-index from download screen: %1.'
});

_.l.set( 'gui', {
   'title'   : 'Offline 4e Database',
   'loading' : 'Loading...',
   'loading1': 'Loading %1'
});

_.l.set( 'action.list', {
   title : "Browse Database",
   link_text : "Browse",

   txt_search_placeholder : "Search keywords. e.g. ranger OR martial   bonus -\"feat bonus\" ",
   chk_search_body : "Search content",
   a_all : "All Category",
   a_category : "%1 (%2)"
});

_.l.set( 'action.view', {
});

_.l.set( 'action.download', {
   title : "Update Database",
   link_text : "Update",
   
   paragraph : [
      "This browser script allows you to download and save compendium entries, then search and list them offline using advanced Google-like syntax.",
      "<b>Requires an active DDI subscription to download any data.</b> <a href='http://www.wizards.com/DnD/Article.aspx?x=dnd/updates' target='_blank'>Official Rules Updates</a>"
   ],
   btn_get_catalog : "Refresh catalog",
   btn_all_list : "Check all",
   btn_list : "Check",
   btn_update_changed : "Get %1 items",
   btn_update_all : "Get all items",
   btn_save : "Save",

   th_category : "Category",
   th_action : " ",
   th_changed : "Changed",
   th_new : "New",
   th_total : "Total",
   
   lbl_progress : "%1/%2",
   
   msg_login  : "A login window will pop up.  Please login and then close the login popup to resume download.  Or press Cancel to stop the update process."
});

_.l.set( 'action.about', {
   title : "Help",
   link_text : "Help",

   h_language : "Language",
   lbl_select_lang : ":", // Please translate as "Please select language:"
   opt_auto    : "(Auto detect)",
   lst_symbol  : "Please select symbols set:",
   opt_original: "Original",
   opt_common  : "Common",
   opt_dingbat : "Dingbat",
   opt_plain   : "Plain",
   hint_basic  : "Basic Attacks (Close, Melee, Ranged, Area)",
   hint_attack : "Non-Basic Attacks (Close, Melee, Ranged, Area)",
   hint_dice   : "Dice faces (Recharge) 1 2 3 4 5 6",
   hint_aura   : "Aura",

   h_license : "License",
   p_license : "This script is free software and licensed under GNU AGPL v3.",
   a_source  : "Source",
   a_email  : "Email",
   lbl_source_manual : "Please right click and select 'View Source' after closing this message, since your browser does not let us do it programmatically.",

   h_get_data : "How to Get data",
   p_get_data :
      "This script comes with no data. To get data, you need a few things, including an active <a href='http://www.wizards.com/DnD/Subscription.aspx'>Dungeons & Dragons Insider subscription</a>. <br/>"+
      "Data can be acquired in a few steps: <br/> <ol> "+
      " <li> Save this HTML to file system and open in Internet Explorer (non-Metro) "+
      " <li> Get catalog "+
      " <li> Get listing of one or more categories "+
      " <li> Get data (may prompt login) "+
      " <li> Save data "+
      "</ol> More information is availble from the download page.",
   
   h_search_data : "How to Search data",
   p_search_data :
      "Type in search terms and it will find results that contains all the terms, in any order, regardless of case. <br/>"+
      "e.g. <kbd>fighter heal</kbd> will search for results that contains <q>Fighter</q> and <q>Heal</q> or <q>Healing</q> or <q>Healer</q>. <br/>"+
      "<br/>"+
      "You can select a category first, to limit search area. Searching by name is also faster then full content search. <br/>"+
      "<br/> <ul>"+
      "<li> To search for a specific term, surround it with double quotes <q>\"</q>. <br/>"+
      " &nbsp; e.g. <kbd>\"extra damage\"</kbd> matches the exact term <q>Extra damage</q>, instead of <q>Extra</q> and <q>Damage</q>. <br/>"+
      "<li> To exclude a term from result, prefix it with minus <q>-</q>. <br/>"+
      " &nbsp; e.g. <kbd>-\"feat bonus\"</kbd> will exclude results containing the term <q>Feat bonus</q>. <br/>"+
      "<li> To specify an OR condition, use an uppercase <q>OR</q>. <br/>"+
      " &nbsp; e.g. <kbd>ranger OR rogue blind</kbd> will search for results containing <q>Blind</q> and either <q>Ranger</q> or <q>Rogue</q>. <br/>"+
      "<li> Use '*' as wild cast. <br/>"+
      " &nbsp; e.g. <kbd>p* bonus</kbd> matches both <q>Proficiency bonus</q> and <q>Power bonus</q>. <br/>"+
      "<li> If you know regular expression, you can use it as terms. <br/>"+
      " &nbsp; e.g. <kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd>. "+
      "</ul>",

   h_move_data : "How to Move data",
   p_move_data : 
      "Acquired data are stored locally, in <q>offline_database_files</q> folder. <br/>"+
      "You just need to open this html to browse and search them without needing to go online. <br/>"+
      "You can lawfully copy this html and the data folder to USB or to smart phone, as long as it is for personal use. <br/>"+
      "<br/>"+
      "Default Android browser does not support local file, but you can use <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera Mobile</a> or <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox Mobile</a>. Chrome would NOT work.<br/> "+
      "Apple iTune forbids lots of things, so for now you will need a jail breaked device."+
      "Please let me know your experience with other smart phones / devices."+
      "<br/>"+
      "You can put everything on personal web server, but initial search speed can be slow, a full search needs ~37MB data.",

   h_history   : "Product History",
   lbl_history : "(If there are no images, please try Chrome or Android browser which support WebP.)",
   lbl_english_only  : "", // Please translate as "This section is English only."
   
   h_version_history : "Version History",
   
   link_homepage : "Home"
});

_.l.set( 'action.license', {
   title : "License",
   link_text : "License"
});