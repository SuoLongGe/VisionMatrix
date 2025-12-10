# VisionMatrix Ctrl+F - 现实世界的Ctrl+F功能

这是一个Android应用，实现了"现实世界的Ctrl+F"功能。用户可以通过输入对象名称（如"杯子"），应用会在相机画面中实时检测并框选出该对象。

## 功能特性

- 📷 实时相机预览（使用CameraX）
- 🔍 目标检测（使用YOLOv8模型）
- 🎯 文本输入搜索（支持中英文）
- 📦 实时绘制检测框
- 🚀 基于NCNN的高性能推理

## 技术栈

- **Android Native**: Kotlin
- **相机库**: CameraX
- **AI框架**: NCNN
- **模型**: YOLOv8n (COCO数据集，80个类别)

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/visionmatrix/ctrlf/
│   │   │   ├── MainActivity.kt          # 主Activity
│   │   │   ├── YOLOv8Detector.kt        # YOLOv8检测器封装
│   │   │   └── DetectionOverlayView.kt  # 检测框绘制视图
│   │   ├── cpp/
│   │   │   ├── yolov8ncnn_jni.cpp       # JNI接口
│   │   │   ├── yolov8.h                 # YOLOv8头文件
│   │   │   ├── yolov8.cpp               # YOLOv8实现
│   │   │   └── CMakeLists.txt           # CMake配置
│   │   ├── assets/
│   │   │   └── yolov8n_ncnn_model/      # 模型文件目录
│   │   │       ├── model.ncnn.param
│   │   │       └── model.ncnn.bin
│   │   └── res/                          # 资源文件
└── build.gradle.kts
```

## 环境要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- Android SDK 24 (Android 7.0) 或更高
- NDK r21e 或更高版本
- CMake 3.18.1 或更高版本

## 依赖库准备

### 1. NCNN库

需要编译NCNN库并放置在项目中：

1. 克隆NCNN仓库：
```bash
git clone https://github.com/Tencent/ncnn.git
cd ncnn
```

2. 编译Android版本：
```bash
mkdir build-android
cd build-android
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI="arm64-v8a" \
    -DANDROID_PLATFORM=android-24 \
    -DCMAKE_BUILD_TYPE=Release \
    -DNCNN_VULKAN=ON \
    ..
make -j4
```

3. 将编译好的库文件复制到项目：
   - 将 `ncnn/build-android/install/include` 复制到 `app/ncnn/include`
   - 将 `ncnn/build-android/install/lib/arm64-v8a/libncnn.a` 复制到 `app/ncnn/lib/arm64-v8a/`
   - 将 `ncnn/build-android/install/lib/armeabi-v7a/libncnn.a` 复制到 `app/ncnn/lib/armeabi-v7a/`

### 2. OpenCV库（可选，用于图像处理）

如果需要使用OpenCV，可以下载OpenCV Android SDK：
1. 从 https://opencv.org/releases/ 下载OpenCV Android SDK
2. 解压后，在 `CMakeLists.txt` 中更新OpenCV路径

### 3. 模型文件

将模型文件复制到assets目录：
```bash
mkdir -p app/src/main/assets/yolov8n_ncnn_model
cp yolov8n_ncnn_model/model.ncnn.param app/src/main/assets/yolov8n_ncnn_model/
cp yolov8n_ncnn_model/model.ncnn.bin app/src/main/assets/yolov8n_ncnn_model/
```

## 编译和运行

1. 打开Android Studio
2. 导入项目
3. 同步Gradle依赖
4. 连接Android设备或启动模拟器
5. 点击运行按钮

## 使用方法

1. 启动应用后，授予相机权限
2. 在底部搜索框输入要搜索的对象名称（如"杯子"、"cup"、"人"等）
3. 点击"搜索"按钮
4. 应用会在相机画面中实时检测并框选出匹配的对象

## 支持的类别

应用支持COCO数据集的80个类别，包括：
- 人物：人、人物
- 交通工具：汽车、自行车、摩托车、公交车等
- 动物：猫、狗、鸟、马等
- 日常用品：杯子、瓶子、手机、笔记本电脑等
- 食物：苹果、香蕉、披萨等
- 家具：椅子、沙发、床等

支持中文和英文输入，系统会自动翻译匹配。

## 注意事项

1. **首次运行**：模型加载可能需要几秒钟时间
2. **性能**：检测性能取决于设备性能，建议在真机上测试
3. **权限**：应用需要相机权限才能正常工作
4. **模型文件**：确保模型文件已正确放置在assets目录中

## 故障排除

### 模型加载失败
- 检查模型文件是否在assets目录中
- 检查文件路径是否正确
- 查看logcat日志获取详细错误信息

### 编译错误
- 确保NDK已正确安装
- 检查CMakeLists.txt中的路径是否正确
- 确保NCNN库已正确编译和放置

### 相机无法启动
- 检查是否授予了相机权限
- 检查设备是否支持CameraX
- 查看logcat日志获取详细错误信息

## 开发计划

- [x] 基础项目结构
- [x] CameraX集成
- [x] NCNN集成
- [x] YOLOv8模型加载
- [x] 目标检测功能
- [x] 检测框绘制
- [x] 文本输入和匹配
- [ ] 性能优化
- [ ] UI美化
- [ ] 多目标同时检测
- [ ] 检测历史记录

## 许可证

本项目遵循AGPL-3.0许可证（YOLOv8模型许可证）。

## 贡献

欢迎提交Issue和Pull Request！


