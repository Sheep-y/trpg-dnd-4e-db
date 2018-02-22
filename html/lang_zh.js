/*
 * lang_zh.js
 *
 * Chinese localization resources
 */

_.l.setLocale( 'zh' );

_.l.set( 'data', {
   'category' : {
      'armor' : '護甲',
      'background' : '背景',
      'class' : '職業',
      'companion' : '伙伴',
      'deity' : '神祇',
      'disease' : '疾病',
      'epicdestiny' : '史詩天命',
      'feat' : '專長',
      'glossary' : '詞目',
      'item' : '裝備',
      'implement' : '法器',
      'monster' : '怪物',
      'paragonpath' : '典範',
      'poison' : '蠱毒',
      'power' : '威能',
      'race' : '種族',
      'ritual' : '法式',
      'theme' : '主題',
      'terrain' : ' 地型',
      'trap' : '陷阱 / 地型',
      'weapon' : '武器'
   },
   'field' : {
      '_CatName' : '類別',
      '_TypeName' : '類型',
      'Action': '動作',
      "ActionType" : '動作',
      'Alignment' : '陣營',
      'Benefit' : '得益',
      'Campaign' : '戰役',
      'Category' : '類別',
      'ClassName': '職業',
      'CombatRole' : '岡位',
      'ComponentCost' : '材料費',
      'Cost' : '價錢',
      'CreatureType' : '類型',
      'DescriptionAttribute' : '能力值',
      'Domains' : '領域',
      'GroupRole' : '地位',
      'KeyAbilities' : '主能力值',
      'KeySkillDescription' : '主技能',
      'Keywords' : '關鍵詞',
      'Level' : '等級',
      'Name' : '名稱',
      'Origin' : '始源',
      'PowerSourceText' : '力量源',
      'Prerequisite' : '前提要求',
      'Price' : '價格',
      'Rarity' : '稀有度',
      'RoleName' : '岡位',
      'Size' : '體型',
      'Skills' : '技能',
      'SourceBook': '書目',
      'Tier' : '層級',
      'TierName' : '層級',
      'Type' : '類型',
      'WeaponType' : '類型'
   }
});

_.l.set( 'error', {
   'old_format' : "數據版本過舊。請重新匯出此數據庫。",
});

_.l.set( 'gui', {
   'title'   : '四版資料庫',
   'top'     : '頂部',
   'loading' : '載入中...',
   'loading1': '載入 %1 中',

   'menu_view_highlight' : '高亮顯示搜尋匹配',
   'update': '新版本',

   ',' : "，",
   '?' : "？",
   'or' : " 或 "
});

_.l.set( 'action.list', {
   'title' : "四版資料庫",
   'link_text' : "瀏覽",
   'result_summary' : "結果",
   'menu_filter_column' : '新增過濾：%1 ',
   'menu_filter_column_only' : '只過濾：%1',

   'txt_search_name_placeholder' : "在此輸入名字，然後選擇分類。",
   'txt_search_full_placeholder' : "在此輸入搜尋關鍵詞。例： ranger OR martial bonus -\"feat bonus\"，然後選擇分類。",
   'btn_search_name' : "名字搜索",
   'btn_search_body' : "全文搜索",
   'a_all' : "全類別",
   'lbl_count' : '%1',

   'lbl_showing' : '%2 項結果',
   'lbl_filter' : '從 %2 項結果中過濾出 %1 項',
   'lbl_page' : '%1，頁 %2/%3',
   'btn_show_page' : '顯示本頁',
   'btn_show_all' : '顯示全部',
   'clear_search' : "<a href='#' onclick='od.action.list.clear_search();'>清除搜尋</a>",
   'clear_filter' : "<a href='#' onclick='od.action.list.clear_filter();'>清除過濾</a>",
   'switch_to_full_text' : "<a href='#' onclick='od.action.list.search(\"full\");'>切換至全文搜索</a>",
   'switch_to_all' : "<a href='#' onclick='od.action.list.a_category();'>全類別搜尋</a>",
   'lbl_no_result' : "無結果。"
});

_.l.set( 'action.view', {
   'menu_quick_lookup' : "檢索 …%1…",
   'menu_name_search' : "名字搜索 %1",
   'menu_full_search' : '全文搜索 "%1"'
});

_.l.set( 'action.about', {
   'title' : "說明",
   'link_text' : "說明",

   'h_language' : "語言",
   'lbl_select_lang' : " 請選擇語言 :", // please translate as " Please select language:" with a leading space
   'lbl_toggle_highlight' : "搜尋結果高亮：",
   'opt_highlight_on' : "顯示",
   'opt_highlight_off' : "不顯示",

   'h_license' : "授權及私隱聲明",
   'p_license' : "此程式稿免費開源，以 GNU AGPL v3 授權發佈。<br/>本應用不收集任何個人資訊。",
   'a_github'  : "主頁",
   'a_email'  : "電郵",

   'p_nodata': "沒有數據。請用<a href='https://github.com/Sheep-y/trpg-dnd-4e-db#readme'>下載器</a>獲取數據。",

   'h_intro' : "這是甚麼？",
   'p_intro' : "這是個由同好者重制的 <a href='http://www.wizards.com/dndinsider/compendium/database.aspx'>D&amp;D Insider 數據庫</a>，以便離線地威力查找四代龍與地下城的資源。 <br/>"+
      "<br/>"+
      "用法很簡單：點擊分類以選擇它，並顯示該分類的列表，然後可以輸入搜尋字詞，按 Enter 或點擊搜尋鍵就會進行搜尋。 <br/>"+
      "符合搜尋的文字預設會高亮顯示。列表可以用任何欄排序，每個欄都可以進一步過濾結果。 <br/>"+
      "如果你使用的是火狐（或任何支援 <a href='https://davidwalsh.name/html5-context-menu'>HTML5 關聯選單</a> 的瀏覽器），你也可以右按任一數據格去設定過濾。 <br/>"+
      "<br/>"+
      "點擊列表的任一行可以檢視條目內文，按著 Ctrl 鍵點擊會開新分頁。 <br/>"+
      "如列表有多項條目，用左右方向鍵或用手指左右劃掃都可以前後行進。 <br/> "+
      "當檢視條目內文時，點擊文字會自動進行快速查找。如有相符的條目會彈出顯示。 <br/>"+
      "你可以用此功能去查找威能關鍵詞或規則字詞，例如 \"Burst\" 或 \"Regeneration\"。 <br/>"+
      "<br/>"+
      "在大部分瀏覽器中，你可以用瀏覽器的前進/後退功能去遍歷瀏覽記錄。 <br/>"+
      "（Chrome 是唯一的例外。它<a href='https://bugs.chromium.org/p/chromium/issues/detail?id=301210'>不允許</a> HTML 檔案管理自身的瀏覽記錄。）",

   'h_search_data' : "如何搜尋",
   'p_search_data' :
      "搜尋有兩種：名字搜尋和全文搜尋。預設是名字搜尋，速度快。全文搜尋則較慢。<br/>"+
      "在搜尋前先選取分類的話，搜尋範圍縮窄了就會搜得較快。 <br/>"+
      "<br/>"+
      "兩種搜尋都會找出包含所有字詞的結果，不論順序，不論大小寫。 <br/>"+
      "例、<a href='?list.full.power=fighter heal'><kbd>fighter heal</kbd></a> 會找出同時包括 <q>Fighter</q> 和 <q>Heal</q> 或 <q>Healing</q> 或 <q>Healer</q> 的資料. <br/>"+
      "<ul>"+
      "<li> 要搜尋特定詞組，可以用半形雙引號 <q>\"</q> 包裹它。 <br/>"+
      " &nbsp; 例、<a href='?list.full.theme=\"extra damage\"'><kbd>\"extra damage\"</kbd></a> 只符合詞組 <q>Extra damage</q>，而不是分成 <q>Extra</q> 和 <q>Damage</q>。 <br/>"+
      "<br/>"+
      "<li> 要自結果排除字詞，可以在前面加半型減號 <q>-</q>。 <br/>"+
      " &nbsp; 例、<a href='?list.full.feat=\"bonus to attack roll\" -\"feat bonus\"'><kbd>-\"feat bonus\"</kbd></a> 會排除包含 <q>Feat bonus</q> 詞組的結果。 <br/>"+
      "<br/>"+
      "<li> 要搜尋獨立的單字，可以在前面加半型加減號 <q>+</q>。 <br/>"+
      " &nbsp; 例、<a href='?list.name.power=%2Bpower'><kbd>+power</kbd></a> 會找出含 <q>power</q> 單字的結果，跳過 <q>empower</q>、<q>powerful</q> <a href='?list.name.power=power'>等字</a>。 <br/>"+
      "<br/>"+
      "<li> 要指定'或者'條件，可用大寫 <q>OR</q>. <br/>"+
      " &nbsp; 例、 <a href='?list.full.power=ranger OR rogue blind'><kbd></a>ranger OR rogue blind</kbd> 會搜尋包含 <q>Blind</q> 以及 <q>Ranger</q> 或 <q>Rogue</q>。 <br/>"+
      "<br/>"+
      "<li> 用半型星號 <q>*</q> 作萬用字符。 <br/>"+
      " &nbsp; 例、<a href='?list.full.ritual=\"p* bonus\"'><kbd>\"p* bonus\"</kbd></a> 同時符合 <q>Proficiency bonus</q> 和 <q>Power bonus</q>。 <br/>"+
      "<br/>"+
      "<li> 等級和價格欄可以施予數字範圍 <br/>"+
      " &nbsp; 例、在等級欄中輸入 <kbd>10-12</kbd> 會得出等級 10, 11, 或 12 的結果。<br/>"+
      " &nbsp; 例、在價格欄中輸入 <kbd><=5k</kbd> 會得出價格 5000 或以下的結果。<br/>"+
      "<br/>"+
      "<li> NIL 代表空白 <br/>"+
      " &nbsp; 例、在專長的前提要求欄中輸入 <kbd>NIL</kbd> 會得出沒有任何前提要求的威能，或用 <kbd>-NIL</kbd> 排除它們。<br/>"+
      "<br/>"+
      "<li> 如果您會用<a href='https://atedev.wordpress.com/2007/11/23/%E6%AD%A3%E8%A6%8F%E8%A1%A8%E7%A4%BA%E5%BC%8F-regular-expression/'>正則表逹式</a>，您可以用它作為字詞進行搜尋。 <br/>"+
      " &nbsp; 例、<a href='?list.full.feat=/(martial|arcane) power( 2)?/ damage bonus'><kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd></a>。"+
      "</ul>",

   'h_move_data' : "手機支援",
   'p_move_data' :
      "數據儲存在 <q id='action_about_lbl_folder'></q> 目錄內。您可以將本 HTML 和數據目錄一起複制到 USB 儲存裝置或智能電話。 <br/>"+
      "預設的安卓瀏覽器可能無法瀏覽離線檔案；您可以用 <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera</a> 或 <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox</a>。Chrome 不一定能開。 <br/> "+
      "<br/>"+
      "你可以把全部檔案當成是一個網站上載到互聯網。在下載器中啓用壓縮可以減少數據量。",

   'h_history' : "產品歷史",
   'lbl_english_only' : "此節只有英文版本。",

   'h_version_history' : "版本歷史",

   'link_homepage' : "主頁"
});

_.l.set( 'action.license', {
   'title' : "授權協議",
   'link_text' : "查看授權"
});