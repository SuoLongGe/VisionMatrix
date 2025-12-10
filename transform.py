from ultralytics import YOLO

# 1. 加载你现在的模型
model = YOLO("E:\RuanChuang\yolov8n.pt")

# 2. 导出为 NCNN 格式 (会自动生成 .param 和 .bin 文件)
# 这一步会把模型转换成安卓手机能高效运行的格式
model.export(format="ncnn")