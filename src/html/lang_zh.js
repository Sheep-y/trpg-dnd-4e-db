/*
 * lang_zh.js
 * Chinese localization resources
 */

_.l.setLocale( 'zh' );

_.l.set( 'data', {
   'category' : {
      'Sample' : '範例',
      'Example' : '例子',
      'Background' : '背景',
      'Class' : '職業',
      'Companion' : '伙伴',
      'Deity' : '神祇',
      'Disease' : '疾病',
      'EpicDestiny' : '史詩天命',
      'Feat' : '專長',
      'Glossary' : '詞目',
      'Item' : '裝備',
      'Monster' : '怪物',
      'ParagonPath' : '典範',
      'Poison' : '蠱毒',
      'Power' : '威能',
      'Race' : '種族',
      'Ritual' : '法式',
      'Terrain' : '地型',
      'Theme' : '主題',
      'Trap' : '陷阱'
   },
   'field' : {
      'Name' : '名稱',
      'Level' : '等級',
      'Category' : '類別',
      'Type' : '類型',
      "PowerSourceText" : '力量源',
      "RoleName" : '岡位',
      "KeyAbilities" : '主能力值',
      "GroupRole" : '地位',
      "Size" : '體型',
      "DescriptionAttribute" : '能力值',
      "ComponentCost" : '材料費',
      "Price" : '價格',
      "KeySkillDescription" : '主技能',
      "Prerequisite" : '前提要求',
      "Campaign" : '戰役',
      "Skills" : '技能',
      "Alignment" : '陣營',
      "Cost" : '花費',
      "Tier" : '層級',
      'ActionType': '動作',
      'ClassName': '職業',
      'SourceBook': '書目'
   }
});

_.l.set( 'error', {
   'updating_data' : 'Error when updating %1 of %2 (%3)',

   // 'no_cross_origin' : 'Cross Origin' // TODO: implement proper cross origin alert
   'ajax_error' : 'Cannot load content (%1) from %2',

   'com_file_security' : 'Cannot save data due to browser security settings. Go to Tools > Internet Options > Security > Custom Level. Enable "Initialize and script ActiveX controls not marked as safe".',
   'file_grant_permission' : 'You may see a prompt. Please grant us the permission update data files.',
   'file_no_api' : 'Only support saving in IE, because JavaScript has no standard for saving portable offline data.',
   'file_cannot_delete' : '無法覆寫 %1',

   'wrong_ext': 'Extended list different from raw list, please re-index from download screen: %1.',
   'need_reindex' : '索引格式過時；請到更新頁重建索引。'
});

_.l.set( 'gui', {
   'title'   : '離線四版資料庫',
   'loading' : 'Loading...',
   'loading1': 'Loading %1'
});

_.l.set( 'action.list', {
   'title' : "瀏覽數據庫",
   'link_text' : "瀏覽",

   'txt_search_placeholder' : "搜尋關鍵詞。例：ranger OR martial   bonus -\"feat bonus\" ",
   'bth_search_name' : "名字搜尋",
   'bth_search_body' : "全文搜尋",
   'bth_search_advanced' : "進階搜尋",
   'a_all' : "全類別",
   'a_category' : "%1 (%2)"
});

_.l.set( 'action.view', {
});

_.l.set( 'action.download', {
   'title' : "更新數據庫",
   'link_text' : "更新",

   'paragraph' : [
      "This browser script allows you to download and save compendium entries, then search and list them offline using advanced Google-like syntax.",
      "<b>Requires an active DDI subscription to download any data.</b> <a href='http://www.wizards.com/DnD/Article.aspx?x=dnd/updates' target='_blank'>Official Rules Updates</a>"
   ],
   'btn_get_catalog' : "刷新列表",
   'btn_all_list' : "全部檢查",
   'btn_list' : "檢查",
   'btn_relist' : "再檢查",
   'btn_delete' : "刪除",
   'btn_reindex': "重建索引",
   'btn_update_changed' : "獲取新/改變條目",
   'btn_update_all' : "獲取所有條目",
   'btn_save' : "儲存",

   'th_category': "類別",
   'th_local'   : "已儲存",
   'th_remote'  : "在線",
   'th_changed' : "異動",
   'th_new'     : "新增",
   'th_status'  : " ",
   'th_commands': " ",

   'lbl_fetching_both' : "提取列表及轉換器中",
   'lbl_fetching_xml' : "提取列表中",
   'lbl_fetching_xsl' : "提取轉換器中",
   'lbl_progress' : "%1/%2",

   'msg_login'  : "一個登入頁即將彈出。請登入然後關閉登入窗口以繼續下載。或者按\"取消\"終止更新程序。"
});

_.l.set( 'action.about', {
   'title' : "關於",
   'link_text' : "關於",

   'h_language' : "語言",
   'lbl_select_lang' : "請選擇語言 :", // please translate as "Please select language:"
   'opt_auto' : "(自動偵察)",
   'lst_symbol'  : "請選擇符號集：",
   'opt_original': "原設",
   'opt_common'  : "常見",
   'opt_dingbat' : "雜錦",
   'opt_plain'   : "純文字",
   'hint_basic'  : "基礎攻擊（近距，近戰，遠程，範圍）",
   'hint_attack' : "基礎攻擊（近距，近戰，遠程，範圍）",
   'hint_dice'   : "骰面（充能） 1 2 3 4 5 6",
   'hint_aura'   : "氣場",

   'h_license' : "授權",
   'p_license' : "此文本是免費軟件，以 GNU AGPL v3 授權發佈。",
   'a_source' : "源碼",
   'a_email'  : "電郵",
   'lbl_source_manual' : "請在關閉此提示後右按並點選'顯示源碼'，因為 IE 不支援，因為您的瀏覽器不能用程序呼叫這功能。",

   'h_get_data' : "如何獲取數據",
   'p_get_data' :
      "此文本不附帶任何數據。要獲取數據，您需要一些東西，當中包括 active <a href='http://www.wizards.com/DnD/Subscription.aspx'>Dungeons & Dragons Insider subscription</a>. <br/>"+
      "要獲取數據，只需以下短短幾步： <br/> <ol> "+
      " <li> 將此 HTML 儲存到檔案系統，並用 Internet Explorer（非 Metro）開啓。"+
      " <li> 下載目錄 "+
      " <li> 下載一類或多類的列表 "+
      " <li> 下載數據（可能會要求登入）"+
      " <li> 儲存數據 "+
      "</ol> 下載頁有更多的詳情。",

   'h_search_data' : "如何搜尋數據",
   'p_search_data' :
      "輸入要找的字詞，就會找出包含所有字詞的結果，不論順序，不論大小寫。 <br/>"+
      "例、<kbd>fighter heal</kbd> 會找出同時包括 <q>Fighter</q> 和 <q>Heal</q> 或 <q>Healing</q> 或 <q>Healer</q> 的結果. <br/>"+
      "<br/>"+
      "您可以先選取一個類別，以收窄搜尋範圍。搜尋名字也會比全文搜尋快。 <br/>"+
      "<br/> <ul>"+
      "<li> 要搜尋特定詞組，可以用半形雙引號 <q>\"</q> 包裹它。 <br/>"+
      " &nbsp; 例、<kbd>\"extra damage\"</kbd> 只符合詞組 <q>Extra damage</q>，而不是分成 <q>Extra</q> 和 <q>Damage</q>。 <br/>"+
      "<li> 要自結果排除字詞，可以在前面加半型減號 <q>-</q>。 <br/>"+
      " &nbsp; 例、<kbd>-\"feat bonus\"</kbd> 會排除包含 <q>Feat bonus</q> 詞組的組果。 <br/>"+
      "<li> 要指定'或者'條件，可用大寫 <q>OR</q>. <br/>"+
      " &nbsp; 例、<kbd>ranger OR rogue blind</kbd> 會搜尋包含 <q>Blind</q> 以及 <q>Ranger</q> 或 <q>Rogue</q>。 <br/>"+
      "<li> 用 '*' 作萬用字符。 <br/>"+
      " &nbsp; 例、<kbd>p* bonus</kbd> 同時符合 <q>Proficiency bonus</q> 和 <q>Power bonus</q>。 <br/>"+
      "<li> 如果您會用正規表逹式，您可以用它代替字詞。 <br/>"+
      " &nbsp; 例、<kbd>/(martial|arcane) power( 2)?/ damage bonus</kbd>. "+
      "</ul>",

   'h_move_data' : "如何搬移數據",
   'p_move_data' :
      "已獲取的數據會儲存在 <q id='action_about_lbl_folder'></q> 目錄之內。 <br/>"+
      "你只需要開啓本 HTML 就可以離線瀏覽和搜尋它們。 <br/>"+
      "在個人使用的前提下，您可以合法地將本 HTML 和數據目錄複制到 USB 或智能電話。 <br/>"+
      "<br/>"+
      "預設的安卓瀏覽器不支援開啓本機檔案，但您可以用 <a href='https://play.google.com/store/apps/details?id=com.opera.browser'>Opera Mobile</a> 或 <a href='https://play.google.com/store/apps/details?id=org.mozilla.firefox'>Firefox Mobile</a>。不能用 Chrome。 <br/> "+
      "蘋果的 iTune 有各種限制，目前您需要已越獄的裝置。"+
      "如果您有對其他智能電話/裝置的經驗，請讓我知道。"+
      "<br/>"+
      "你可以把東西都放到個人網頁伺服器，不過初始的搜尋可能很慢，完整全文搜尋需要 ~37MB 數據。",

   'h_history' : "產品歷史",
   'lbl_history' : "（如若沒有看見圖片，請試用支援 WebP 格式的 Chrome 或 Android 瀏覽器。）",
   'lbl_english_only' : "此章節只有英文版本。",

   'h_version_history' : "版本歷史",

   'link_homepage' : "主頁"
});

_.l.set( 'action.license', {
   'title' : "授權協議",
   'link_text' : "授權"
});