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
      "ActionType" : 'Action',
      'ClassName': 'Class',
      "CombatRole" : 'Role',
      "ComponentCost" : 'Component',
      'CreatureType' : 'Type',
      "DescriptionAttribute" : 'Attribute',
      "GroupRole" : 'Group',
      "KeyAbilities" : 'Abilities',
      "KeySkillDescription" : 'Key Skill',
      "PowerSourceText" : 'Power Source',
      "RoleName" : 'Role',
      'SourceBook': 'Source',
      'TierName': 'Tier'
   }
});

_.l.set( 'error', {
   'old_format' : "Data format is outdated. Please re-export this viewer.",
});

_.l.set( 'gui', {
   'title'   : '4e Database',
   'top'   : 'Top',
   'loading' : 'Loading...',
   'loading1': 'Loading %1',

   'menu_view_highlight' : 'Highlight search terms',
   'update': 'New Version',

   ',' : ", ",
   '?' : "?",
   'or' : " or "
});

_.l.set( 'action.list', {
   'title' : "4e Database",
   'link_text' : "Browse",
   'result_summary' : "Result",
   'menu_filter_column' : 'Add %1 to filter',
   'menu_filter_column_only' : 'Filter only %1',

   'txt_search_name_placeholder' : "Type name here and then select category.",
   'txt_search_full_placeholder' : "Type search keywords here, e.g. ranger OR martial bonus -\"feat bonus\", and then select category.",
   'btn_search_name' : "<u>N</u>ame Search",
   'btn_search_body' : "<u>F</u>ull Search",
   'a_all' : "Everything",
   'lbl_count' : '%1',

   'lbl_showing' : '%1 result(s)',
   'lbl_filter'   : 'Filter left %1 result(s) out of %2',
   'lbl_page' : '%1 Page %2/%3',
   'clear_search' : "<a href='#' onclick='od.action.list.clear_search();'>Clear search</a>",
   'clear_filter' : "<a href='#' onclick='od.action.list.clear_filter();'>Clear filter</a>",
   'switch_to_full_text' : "<a href='#' onclick='od.action.list.search(\"full\");'>Switch to full search</a>",
   'switch_to_all' : "<a href='#' onclick='od.action.list.a_category();'>Search everything</a>",
   'lbl_no_result' : "No result. "
});

_.l.set( 'action.view', {
   'menu_quick_lookup' : "Lookup …%1…",
   'menu_name_search' : "Name Search %1",
   'menu_full_search' : 'Full Search "%1"'
});

_.l.set( 'action.about', {
   'title' : "Help",
   'link_text' : "Help",

   'h_language' : "Language",
   'lbl_select_lang' : ":", // Please translate as " Please select language:" with a leading space
   'lbl_toggle_highlight' : "Search term highlight:",
   'opt_highlight_on' : "On",
   'opt_highlight_off' : "Off",

   'h_license' : "License",
   'p_license' : "This program is free software and is licensed under GNU AGPL v3.<br/>This program does not collect any personal information.",
   'a_github'  : "Homepage",
   'a_email'  : "Email",

   'p_nodata':
      "There is no data. Please fetch with <a href='https://github.com/Sheep-y/trpg-dnd-4e-db#readme'>downloader</a>.",

   'h_intro' : "What is this?",
   'p_intro' :
      "This is a fan remake of the official <a href='http://www.wizards.com/dndinsider/compendium/database.aspx'>D&amp;D Insider's Compendium</a> for powerful, offline search of 4<sup>th</sup> edition D&amp;D data. <br/>"+
      "<br/>"+
      "Usage is simple: click on a category to select it and see listing, then / or input search term(s) and press enter or click a search button to search. <br/>"+
      "Text that match the terms is highlighted by default, and the listing can be sorted by any column and filtered by each column. <br/>"+
      "On Firefox (or any browser that supports <a href='https://davidwalsh.name/html5-context-menu'>HTML5 context menu</a>), you can also right click on a cell to set filter with it. <br/>"+
      "<br/>"+
      "Click on a row to see the content of an entry, or Ctrl+Click to open in new tab. <br/>"+
      "If the listing has multiple entries, you can use the left / right key or swipe to walk through the list. <br/> "+
      "When viewing entry content, clicking any text will start a quick lookup.  If any matching entry is found, a list will pop up. <br/>"+
      "You can use this feature to quickly lookup power keywords or rule terms like \"Burst\" or \"Regeneration\". <br/>"+
      "<br/>"+
      "On most browsers, you can use the browser's forward and backward feaure to transverse browse history. <br/>"+
      "(Chrome is the only exception, which <a href='https://bugs.chromium.org/p/chromium/issues/detail?id=301210'>does not allow</a> HTML file to manage its own history.)",

   'h_search_data' : "How to Search",
   'p_search_data' :
      "There are two types of search: name only search and full text search. <br/>"+
      "Name search (default) is fast.  Full search is bigger and slower. <br/>"+
      "A search will run faster if a category is selected before the search, to limit its scope. <br/>"+
      "<br/>"+
      "Both searches find results that contains every terms, in any order, regardless of case. <br/>"+
      "e.g. <a href='?list.full.power=fighter heal'><kbd>fighter heal</kbd></a> will search for data that contains <q>Fighter</q> and <q>Heal</q> or <q>Healing</q> or <q>Healer</q>. <br/>"+
      "<ul>"+
      "<li> To search for a multi-word term, surround it with double quotes <q>\"</q>. <br/>"+
      " &nbsp; e.g. <a href='?list.full.theme=\"extra damage\"'><kbd>\"extra damage\"</kbd></a> matches the exact term <q>Extra damage</q>, instead of <q>Extra</q> and <q>Damage</q>. <br/>"+
      "<br/>"+
      "<li> To exclude a term from result, prefix it with minus <q>-</q>. <br/>"+
      " &nbsp; e.g. <a href='?list.full.feat=\"bonus to attack roll\" -\"feat bonus\"'><kbd>-\"feat bonus\"</kbd></a> will exclude results containing the term <q>Feat bonus</q>. <br/>"+
      "<br/>"+
      "<li> To search for a whole word, prefix it with plus <q>+</q>. <br/>"+
      " &nbsp; e.g. <a href='?list.name.power=%2Bpower'><kbd>+power</kbd></a> will include result containing the word <q>power</q>, <a href='?list.name.power=power'>but not</a> <q>empower</q> or <q>powerful</q>. <br/>"+
      "<br/>"+
      "<li> To specify an OR condition, use an uppercase <q>OR</q>. <br/>"+
      " &nbsp; e.g. <a href='?list.full.power=ranger OR rogue blind'><kbd>ranger OR rogue blind</kbd></a> will search for results containing <q>Blind</q> and either <q>Ranger</q> or <q>Rogue</q>. <br/>"+
      "<br/>"+
      "<li> Use asterisk <q>*</q> as wild cast. <br/>"+
      " &nbsp; e.g. <a href='?list.full.ritual=\"p* bonus\"'><kbd>\"p* bonus\"</kbd></a> matches both <q>Proficiency bonus</q> and <q>Power bonus</q>. <br/>"+
      "<br/>"+
      "<li> Number range is supported in level and cost column. <br/>"+
      " &nbsp; e.g. <kbd>10-12</kbd> in the level field will yield results that is level 10, 11, or 12. <br/>"+
      " &nbsp; e.g. <kbd><=5k</kbd> in the cost field will yield results that cost at most 5000. <br/>"+
      "<br/>"+
      "<li> NIL is empty <br/>"+
      " &nbsp; e.g. <kbd>NIL</kbd> in the prerequisite field of feats will yield feats that has no requirements, or <kbd>-NIL</kbd> to exclude them. <br/>"+
      "<br/>"+
      "<li> If you know <a href='http://www.regular-expressions.info/quickstart.html'>regular expression</a>, you can use it as a term. <br/>"+
      " &nbsp; e.g. <a href='?list.full.feat=/(martial|arcane) power( 2)?/ damage bonus'><kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd></a>. "+
      "</ul>",

   'h_move_data' : "Viewing on Mobile",
   'p_move_data' :
      "Acquired data are stored locally, in <q id='action_about_lbl_folder'></q> folder. "+
      "You can copy this html file and the data folder together to USB drive or to smart phone. <br/>"+
      "Default Android browser may be unable to browser offline file; you can use <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera</a> or <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox</a>. Chrome may NOT work.<br/> "+
      "<br/>"+
      "You can also upload it to Internet as a web site.  Enabling compression in the downloader will result in smaller data.",

   'h_history'   : "Product History",
   'lbl_english_only'  : "", // Please translate as "This section is English only."

   'h_version_history' : "Version History",

   'link_homepage' : "Home"
});

_.l.set( 'action.license', {
   'title' : "License",
   'link_text' : "View License"
});