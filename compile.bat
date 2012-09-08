@echo off
cd src
copy html_doctype.html+style.css+html_header.html+code_util.js+code_gui.js+action_index.html+action_download.html+action_list.html+action_view.html+data_model.js+data_read.js+data_download.js+data_index.js+data_write.js+data_search.js+html_footer.html offline_ddi.html
cd ..
del offline_ddi.html
move src\offline_ddi.html .
