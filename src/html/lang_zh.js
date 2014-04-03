/*                                                                              ex: softtabstop=3 shiftwidth=3 tabstop=3 expandtab
 * lang_zh.js
 *
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
      "TierName" : '層級',
      'ActionType': '動作',
      'ClassName': '職業',
      'SourceBook': '書目'
   }
});

_.l.set( 'error', {
   'updating_data' : '更新 %2 的 %1 時發生錯誤 (%3)',

   // 'no_cross_origin' : 'Cross Origin' // TODO: implement proper cross origin alert
   'ajax_error' : '無法載入 %2 的 %1 的內容。',

   'com_file_security' : '瀏覽器安全等級過高，無法儲存數據。請到 Tools > Internet Options > Security > Custom Level 啟用 "Initialize and script ActiveX controls not marked as safe".',
   'file_grant_permission' : '你可能會看見一個警示。請給予我們更新數據檔案的權限。',
   'file_no_api' : '需要用 IE 才能更新，因為 JavaScript 不支援寫入本機檔案。',
   'file_cannot_delete' : '無法覆寫 %1',

   'wrong_ext': 'Extended list different from raw list, please re-index from download screen: %1.',
   'need_reindex' : '數據格式過時；請到更新頁對所有資料重建索引。',
   'need_redown'  : '數據格式過時；請刪除所有資料並重新下載。',
   'inconsistent_category' : "分類'%1'數據有錯誤(%2)。請到更新頁重建索引或者把它刪除後重新下載。"
});

_.l.set( 'gui', {
   'title'   : '四版資料庫 - ',
   'loading' : '載入中...',
   'loading1': '載入 %1 中',
   'top'     : '頂部'
});

_.l.set( 'action.list', {
   'title' : "瀏覽",
   'link_text' : "瀏覽",
   'result_summary' : "結果",

   'txt_search_name_placeholder' : "輸入要搜尋的名字（可選），然後選擇分類。",
   'txt_search_full_placeholder' : "搜尋關鍵詞。例： ranger OR martial bonus -\"feat bonus\" ",
   'btn_search_name' : "名字搜索",
   'btn_search_body' : "全文搜索",
   'a_all' : "全類別",
   'lbl_count' : '%1／%2',

   'lbl_no_result' : "無結果"
});

_.l.set( 'action.view', {
   'title' : "View Data"
});

_.l.set( 'action.update', {
   'title' : "更新",
   'link_text' : "更新",

   'paragraph' : [
      "此網頁讓您得以下載和儲存<a href='http://www.wizards.com/dndinsider/compendium/database.aspx'>數據庫</a的條目，然後不論在線離線皆可以用強力的類谷哥語法去搜尋和列出它們。",
      "<b>需要有效的 <a href='http://www.wizards.com/DnD/Subscription.aspx'>DDI 賬戶</a>才能下載或更新任何數據。</b>"
   ],
   'btn_get_catalog' : "刷新列表",
   'btn_all_list' : "全部檢查",
   'btn_list' : "檢查",
   'btn_relist' : "再檢查",
   'btn_delete' : "刪除",
   'btn_reindex': "重建索引",
   'btn_update' : "獲取新/改變條目",
   'btn_update_all' : "獲取所有條目",
   'btn_save' : "儲存",

   'th_category': "類別",
   'th_local'   : "已儲存",
   'th_remote'  : "在線",
   'th_changed' : "異動",
   'th_new'     : "新增",
   'th_status'  : " ",
   'th_commands': " ",

   'lbl_new_item'     : '%2 的新增項目：%1',
   'lbl_changed_item' : '%2 的異動項目：%1',
   'btn_close' : '關閉',

   'lbl_fetching_both' : "提取列表及轉換器中",
   'lbl_fetching_xml' : "提取列表中",
   'lbl_fetching_xsl' : "提取轉換器中",
   'lbl_progress' : "%1／%2",

   'err_online':
      "<b class='red'>請先將此網頁儲存成本機檔案，開啟它，才可以進行下載或更新。</b>",
   'err_nonie':
      "<b class='red'>限使用 <a href='http://ie.microsoft.com/'>Internet Explorer (9+)</a> 才可以進行下載或更新。</b>",
   'err_activex':
      "<b class='red'>限使用非 Metro IE 及需啟用 ActiveX 才可以進行下載或更新。</b>",

   'msg_login'  : "一個登入頁即將彈出。請*登入並關閉*登入窗口以繼續下載。"
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

   'h_license' : "授權及私隱聲明",
   'p_license' : "此程式稿免費開源，以 GNU AGPL v3 授權發佈。<br/>本程式稿不收集任何可以辨認個人的資訊。",
   'a_source' : "檢視源碼",
   'a_github'  : "Github 源碼",
   'a_email'  : "電郵",
   'lbl_source_manual' : "請在關閉此提示後右按並點選'顯示源碼'，因為 IE 不支援，因為您的瀏覽器不能用程序呼叫這功能。",

   'h_get_data' : "如何獲取數據",
   'p_get_data' :
      "此文本不附帶任何數據。要獲取數據，您需要一些東西，當中包括 active <a href='http://www.wizards.com/DnD/Subscription.aspx'>Dungeons & Dragons Insider subscription</a>. <br/>"+
      "要獲取數據，只需以下短短幾步： <br/> <ol> "+
      " <li> 將此 HTML 儲存到您的電腦，並用 Internet Explorer（桌面版）開啓。"+
      " <li> 下載目錄 "+
      " <li> 下載一類或多類的列表 "+
      " <li> 下載數據（可能會要求登入）"+
      " <li> 儲存數據 "+
      "</ol> 更新頁有更多的詳情。",

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
   'lbl_english_only' : "此章節只有英文版本。",

   'h_version_history' : "版本歷史",

   'link_homepage' : "主頁"
});

_.l.set( 'action.license', {
   'title' : "授權協議",
   'link_text' : "查看授權"
});