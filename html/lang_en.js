/*
 * lang_en.js
 *
 * English localization resources
 */

_.l.setLocale( 'en' );


_.l.set( 'data', {
   'category' : {
      'epicdestiny' : 'Epic Destiny',
      'paragonpath' : 'Paragon Path',
      'trap'        : 'Trap / Terrain'
   },
   'field' : {
      "_CatName" : "Category",
      "_TypeName" : "Type",
      "PowerSourceText" : 'Power Source',
      "CombatRole" : 'Role',
      "RoleName" : 'Role',
      "KeyAbilities" : 'Abilities',
      "GroupRole" : 'Group',
      "DescriptionAttribute" : 'Attribute',
      "ComponentCost" : 'Component',
      "KeySkillDescription" : 'Key Skill',
      'ClassName': 'Class',
      'SourceBook': 'Source',
      'TierName': 'Tier'
   }
});

_.l.set( 'error', {
   'old_format' : "Data format is outdated. Please re-export this viewer or revert to 3.5.1",
   'inconsistent_category' : "Category '%1' has inconsistent data (%2).  Please re-export this viewer."
});

_.l.set( 'gui', {
   'title'   : '4e Database',
   'top'   : 'Top',
   'loading' : 'Loading...',
   'loading1': 'Loading %1',

   'menu_view_highlight' : 'On/off search term highlights'
});

_.l.set( 'action.list', {
   'title' : "Browse",
   'link_text' : "Browse",
   'result_summary' : "Result",

   'txt_search_name_placeholder' : "Type name and then select category.",
   'txt_search_full_placeholder' : "Type search keywords. e.g. ranger OR martial bonus -\"feat bonus\" and then select category.",
   'btn_search_name' : "<u>N</u>ame Search",
   'btn_search_body' : "<u>F</u>ull Search",
   'a_all' : "All Category",
   'lbl_count' : '%1 / %2',

   'lbl_showing' : '%1 result(s)',
   'lbl_filter'   : 'Filter left %1 result(s) out of %2',
   'lbl_page' : '頁數 %1/%2',
   'lbl_no_result' : "No result"
});

_.l.set( 'action.view', {
   'title' : "View %1 Data"
});

_.l.set( 'action.about', {
   'title' : "Help",
   'link_text' : "Help",

   'h_language' : "Language",
   'lbl_select_lang' : ":", // Please translate as "Please select language:"
   'opt_auto'    : "(Auto detect)",
   'lbl_toggle_highlight' : "Search term highlight:",
   'opt_highlight_on' : "On",
   'opt_highlight_off' : "Off",

   'h_license' : "License",
   'p_license' : "This script is free software and is licensed under GNU AGPL v3.<br/>This script does not collect any personally identifiable information.",
   'a_github'  : "Githib Source",
   'a_email'  : "Email",
   'lbl_source_manual' : "Please right click and select 'View Source' after closing this message, since your browser does not let us do it programmatically.",

   'h_get_data' : "How to Get data",
   'p_get_data' :
      "This script comes with no data. "+
      "Data can be acquired with a <a href='https://github.com/Sheep-y/trpg-dnd-4e-db#readme'>downloader</a> if you have an active (paid) <a href='http://ddi.wizards.com/'>Dungeons &amp; Dragons insider</a> subscription and a computer.",

   'h_search_data' : "How to Search data",
   'p_search_data' :
      "Type in search terms and it will find results that contains all the terms, in any order, regardless of case. <br/>"+
      "e.g. <kbd>fighter heal</kbd> will search for results that contains <q>Fighter</q> and <q>Heal</q> or <q>Healing</q> or <q>Healer</q>. <br/>"+
      "<br/>"+
      "You can select a category first, to limit search area.  Search terms are highlighted by default, and can be disabled in firefox through right click menu. <br/>"+
      "<br/> <ul>"+
      "<li> To search for a specific term, surround it with double quotes <q>\"</q>. <br/>"+
      " &nbsp; e.g. <kbd>\"extra damage\"</kbd> matches the exact term <q>Extra damage</q>, instead of <q>Extra</q> and <q>Damage</q>. <br/>"+
      "<li> To exclude a term from result, prefix it with minus <q>-</q>. <br/>"+
      " &nbsp; e.g. <kbd>-\"feat bonus\"</kbd> will exclude results containing the term <q>Feat bonus</q>. <br/>"+
      "<li> To search for a whole word, prefix it with plus <q>+</q>. <br/>"+
      " &nbsp; e.g. <kbd>+power</kbd> will include result containing the word <q>power</q>, but not <q>empower</q> or <q>powerful</q>. <br/>"+
      "<li> To specify an OR condition, use an uppercase <q>OR</q>. <br/>"+
      " &nbsp; e.g. <kbd>ranger OR rogue blind</kbd> will search for results containing <q>Blind</q> and either <q>Ranger</q> or <q>Rogue</q>. <br/>"+
      "<li> Use '*' as wild cast. <br/>"+
      " &nbsp; e.g. <kbd>p* bonus</kbd> matches both <q>Proficiency bonus</q> and <q>Power bonus</q>. <br/>"+
      "<li> If you know regular expression, you can use it as terms. <br/>"+
      " &nbsp; e.g. <kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd>. "+
      "</ul>",

   'h_move_data' : "How to Move data",
   'p_move_data' :
      "Acquired data are stored locally, in <q id='action_about_lbl_folder'></q> folder. <br/>"+
      "You just need to open this html to browse and search them without needing to go online. <br/>"+
      "You can lawfully copy this html and the data folder to USB or to smart phone, as long as it is for personal use. <br/>"+
      "<br/>"+
      "Default Android browser does not support local file, but you can use <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera Mobile</a> or <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox Mobile</a>. Chrome would NOT work.<br/> "+
      "Apple iTune forbids lots of things, so for now you will need a jail breaked device. "+
      "Please let me know your experience with other smart phones / devices. "+
      "<br/>"+
      "You can also put everything on a personal web server, if you have one, but initial search speed can be slow, a full search needs ~37MB data.",

   'h_history'   : "Product History",
   'lbl_english_only'  : "", // Please translate as "This section is English only."

   'h_version_history' : "Version History",

   'link_homepage' : "Home"
});

_.l.set( 'action.license', {
   'title' : "License",
   'link_text' : "View License"
});