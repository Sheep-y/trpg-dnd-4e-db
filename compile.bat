@echo off
cd src
copy html_doctype.html+style.css+html_header.html+code_util.js+code_gui.js+data_model.js+data_read.js+data_index.js+data_write.js+action_download.html+action_list.html+action_view.html+html_footer.html offline_ddi.html
cd ..
del offline_ddi.html
move src\offline_ddi.html .
