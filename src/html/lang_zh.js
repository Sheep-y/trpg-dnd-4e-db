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

   'wrong_ext': 'Extended list different from raw list, please re-index from download screen: %1.'
});

_.l.set( 'gui', {
   'loading': 'Loading...',
   'loading1': 'Loading %1'
});

_.l.set( 'action.list', {
   title : "瀏覽數據庫",
   link_text : "瀏覽",

   txt_search_placeholder : "搜尋關鍵詞。例：ranger OR martial   bonus -\"feat bonus\" ",
   chk_search_body : "搜尋內文",
   a_all : "全類別",
   a_category : "%1 (%2)"
});

_.l.set( 'action.view', {
});

_.l.set( 'action.download', {
   title : "更新數據庫",
   link_text : "更新",
   
   paragraph : [
      "This browser script allows you to download and save compendium entries, then search and list them offline using advanced Google-like syntax.",
      "<b>Requires an active DDI subscription to download any data.</b> <a href='http://www.wizards.com/DnD/Article.aspx?x=dnd/updates' target='_blank'>Official Rules Updates</a>"
   ],
   btn_get_catalog : "刷新列表",
   btn_all_list : "Check all",
   btn_list : "Check",
   btn_update_changed : "Get %1 items",
   btn_update_all : "Get all items",
   btn_save : "儲存",

   th_category : "類別",
   th_action : " ",
   th_changed : "異動",
   th_new : "新增",
   th_total : "合共",
   
   lbl_progress : "%1/%2",
   
   msg_login  : "一個登入頁即將彈出。請登入然後關閉登入窗口以繼續下載。或者按\"取消\"終止更新程序。"
});

_.l.set( 'action.about', {
   title : "關於",
   link_text : "關於",
   
   h_language : "語言",
   lbl_select_lang : "請選擇語言 :", // please translate as "Please select language:"
   opt_auto : "(自動偵察)", 
   
   h_license : "授權",
   p_license : "此文本是免費軟件，以GNU AGPL v3 授權發佈。",
   a_source : "源碼",
   lbl_source_manual : "請在關閉此提示後右按並點選'顯示源碼'，因為 IE 不支援，因為您的瀏覽器不能用程序呼叫這功能。",
   
   h_help : "使用方法",
   h_version_history : "版本歷史",
   h_history : "產品歷史",
   lbl_history : "（如若圖片沒有顯示，請試用支援 WebP 格式的 Chrome 或 Android 瀏覽器。）",
   lbl_english_only : "此章節只有英文版本。",
   
   link_homepage : "主頁"
});

_.l.set( 'action.license', {
   title : "授權協議",
   link_text : "授權",
});