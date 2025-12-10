#!/bin/bash
# 复制模型文件到assets目录

echo "正在复制模型文件..."

# 创建assets目录（如果不存在）
mkdir -p app/src/main/assets/yolov8n_ncnn_model

# 复制模型文件
cp yolov8n_ncnn_model/model.ncnn.param app/src/main/assets/yolov8n_ncnn_model/
cp yolov8n_ncnn_model/model.ncnn.bin app/src/main/assets/yolov8n_ncnn_model/

echo "模型文件复制完成！"
echo "文件位置: app/src/main/assets/yolov8n_ncnn_model/"


