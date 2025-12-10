@echo off
REM 复制模型文件到assets目录

echo 正在复制模型文件...

REM 创建assets目录（如果不存在）
if not exist "app\src\main\assets\yolov8n_ncnn_model" mkdir "app\src\main\assets\yolov8n_ncnn_model"

REM 复制模型文件
copy /Y "yolov8n_ncnn_model\model.ncnn.param" "app\src\main\assets\yolov8n_ncnn_model\"
copy /Y "yolov8n_ncnn_model\model.ncnn.bin" "app\src\main\assets\yolov8n_ncnn_model\"

echo 模型文件复制完成！
echo 文件位置: app\src\main\assets\yolov8n_ncnn_model\


