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
   'old_format' : "Data format is outdated. Please re-export this viewer.",
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
   'a_all' : "Everything",
   'lbl_count' : '%1',

   'lbl_showing' : '%1 result(s)',
   'lbl_filter'   : 'Filter left %1 result(s) out of %2',
   'lbl_page' : '%1 Page %2/%3',
   'lbl_no_result' : "No result"
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
   'p_license' : "This app is free software and is licensed under GNU AGPL v3.<br/>This app does not collect any personal information.",
   'a_github'  : "Homepage",
   'a_email'  : "Email",

   'h_intro' : "What is this?",
   'p_intro' :
      "This is a fan remake of the official <a href='http://www.wizards.com/dndinsider/compendium/database.aspx'>D&amp;D Insider's Compendium</a> for searching and browsing 4<suo>th</sup> edition D&amp;D data. <br><br>"+
      "If you see no data or incomplete data, data can be acquired with the <a href='https://github.com/Sheep-y/trpg-dnd-4e-db#readme'>downloader</a>.",

   'h_search_data' : "How to Search",
   'p_search_data' :
      "Type in search terms to find results that contains all the terms, in any order, regardless of case. <br/>"+
      "e.g. <kbd>fighter heal</kbd> will search for data that contains <q>Fighter</q> and <q>Heal</q> or <q>Healing</q> or <q>Healer</q>. <br/>"+
      "<br/>"+
      "You can select a category first, to limit search area.  Search terms are highlighted by default, and can be disabled from the options. <br/>"+
      "<br/> <ul>"+
      "<li> To search for a multi-word term, surround it with double quotes <q>\"</q>. <br/>"+
      " &nbsp; e.g. <kbd>\"extra damage\"</kbd> matches the exact term <q>Extra damage</q>, instead of <q>Extra</q> and <q>Damage</q>. <br/>"+
      "<li> To exclude a term from result, prefix it with minus <q>-</q>. <br/>"+
      " &nbsp; e.g. <kbd>-\"feat bonus\"</kbd> will exclude results containing the term <q>Feat bonus</q>. <br/>"+
      "<li> To search for a whole word, prefix it with plus <q>+</q>. <br/>"+
      " &nbsp; e.g. <kbd>+power</kbd> will include result containing the word <q>power</q>, but not <q>empower</q> or <q>powerful</q>. <br/>"+
      "<li> To specify an OR condition, use an uppercase <q>OR</q>. <br/>"+
      " &nbsp; e.g. <kbd>ranger OR rogue blind</kbd> will search for results containing <q>Blind</q> and either <q>Ranger</q> or <q>Rogue</q>. <br/>"+
      "<li> Use '*' as wild cast. <br/>"+
      " &nbsp; e.g. <kbd>p* bonus</kbd> matches both <q>Proficiency bonus</q> and <q>Power bonus</q>. <br/>"+
      "<li> Number range is supported in level and cost column. <br/>"+
      " &nbsp; e.g. <kbd>10-12</kbd> in the level field will yield data that is between level 10 to 12. <br/> The cost and level of an item includes all level in, e.g. a Holy Avenger is both level 25 and level 30.<br/>"+
      "<li> If you know regular expression, you can use it as terms. <br/>"+
      " &nbsp; e.g. <kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd>. "+
      "</ul>",

   'h_move_data' : "Viewing on Mobile",
   'p_move_data' :
      "Acquired data are stored locally, in <q id='action_about_lbl_folder'></q> folder. <br/>"+
      "You can lawfully copy this html file and the data folder to USB drive or to smart phone, as long as it is for personal use. <br/>"+
      "<br/>"+
      "Default Android browser may not allow browsing local file, but you can use <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera</a> or <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox</a>. Chrome may NOT work.<br/> "+
      "Apple forbids lots of things.  Please let me know if you find a way to read on iOS. "+
      "<br/>"+
      "You can also upload it to Internet as a web site, but search speed can be slow: a full search needs ~24MB data.",

   'h_history'   : "Product History",
   'lbl_english_only'  : "", // Please translate as "This section is English only."

   'h_version_history' : "Version History",

   'link_homepage' : "Home"
});

_.l.set( 'action.license', {
   'title' : "License",
   'link_text' : "View License"
});