# 项目设置指南

## 快速开始

### 1. 复制模型文件

模型文件已经复制到 `app/src/main/assets/yolov8n_ncnn_model/` 目录。

如果模型文件丢失，可以运行：
- Windows: `.\copy_models.bat`
- Linux/Mac: `bash copy_models.sh`

### 2. 准备NCNN库

#### 方法一：使用预编译的NCNN库（推荐）

**选择版本：**
- **推荐**：`ncnn-android-vulkan.zip` - 支持GPU加速，性能更好
- **备选**：`ncnn-android.zip` - 基础版本，不支持GPU（如果设备不支持Vulkan或想先测试基础功能）

**下载步骤：**

1. 访问 [NCNN Releases](https://github.com/Tencent/ncnn/releases)
2. 下载 `ncnn-android-vulkan.zip`（推荐）或 `ncnn-android.zip`
3. 解压下载的zip文件

**设置步骤：**

解压后的目录结构应该类似：
```
ncnn-android-vulkan/
├── include/
│   └── ncnn/
│       ├── allocator.h
│       ├── benchmark.h
│       └── ... (其他头文件)
├── lib/
│   ├── arm64-v8a/
│   │   ├── libncnn.a
│   │   └── libncnn.so (可选)
│   ├── armeabi-v7a/
│   │   ├── libncnn.a
│   │   └── libncnn.so (可选)
│   └── ... (其他架构)
└── ...
```

4. 将解压后的内容复制到项目目录：
   ```bash
   # 创建ncnn目录
   mkdir -p app/ncnn
   
   # 复制include目录
   cp -r ncnn-android-vulkan/include app/ncnn/
   
   # 复制lib目录
   cp -r ncnn-android-vulkan/lib app/ncnn/
   ```

   或者手动复制：
   - 将 `ncnn-android-vulkan/include` 复制到 `app/ncnn/include`
   - 将 `ncnn-android-vulkan/lib` 复制到 `app/ncnn/lib`

**最终目录结构应该是：**
```
app/
└── ncnn/
    ├── include/
    │   └── ncnn/
    │       └── ... (所有头文件)
    └── lib/
        ├── arm64-v8a/
        │   └── libncnn.a
        └── armeabi-v7a/
            └── libncnn.a
```

#### 方法二：自行编译NCNN库

1. 克隆NCNN仓库：
```bash
git clone https://github.com/Tencent/ncnn.git
cd ncnn
```

2. 编译Android版本（arm64-v8a）：
```bash
mkdir build-android-arm64
cd build-android-arm64
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI="arm64-v8a" \
    -DANDROID_PLATFORM=android-24 \
    -DCMAKE_BUILD_TYPE=Release \
    -DNCNN_VULKAN=OFF \
    -DNCNN_BUILD_TOOLS=OFF \
    -DNCNN_BUILD_EXAMPLES=OFF \
    ..
make -j4
```

3. 编译Android版本（armeabi-v7a）：
```bash
cd ..
mkdir build-android-armv7
cd build-android-armv7
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI="armeabi-v7a" \
    -DANDROID_PLATFORM=android-24 \
    -DCMAKE_BUILD_TYPE=Release \
    -DNCNN_VULKAN=OFF \
    -DNCNN_BUILD_TOOLS=OFF \
    -DNCNN_BUILD_EXAMPLES=OFF \
    ..
make -j4
```

4. 复制编译好的文件：
```bash
# 创建目录结构
mkdir -p app/ncnn/include
mkdir -p app/ncnn/lib/arm64-v8a
mkdir -p app/ncnn/lib/armeabi-v7a

# 复制头文件
cp -r ncnn/src/* app/ncnn/include/

# 复制库文件
cp ncnn/build-android-arm64/install/lib/libncnn.a app/ncnn/lib/arm64-v8a/
cp ncnn/build-android-armv7/install/lib/libncnn.a app/ncnn/lib/armeabi-v7a/
```

### 3. 更新CMakeLists.txt路径

如果NCNN库的路径与默认设置不同，请修改 `app/src/main/cpp/CMakeLists.txt` 中的路径：

```cmake
# 修改这些路径以匹配你的NCNN库位置
set(NCNN_INCLUDE_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn/include)
set(NCNN_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn/lib)
```

### 4. 在Android Studio中打开项目

1. 打开Android Studio
2. 选择 "Open an Existing Project"
3. 选择项目根目录
4. 等待Gradle同步完成

### 5. 编译和运行

1. 连接Android设备或启动模拟器（API 24+）
2. 点击运行按钮（绿色三角形）
3. 首次运行可能需要几分钟来编译NDK代码

## 常见问题

### Q: 编译错误 "找不到ncnn.h"
A: 检查NCNN库是否正确放置在 `app/ncnn/include/` 目录，并确保CMakeLists.txt中的路径正确。

### Q: 链接错误 "找不到libncnn.a"
A: 检查NCNN库文件是否在 `app/ncnn/lib/arm64-v8a/` 和 `app/ncnn/lib/armeabi-v7a/` 目录中。

### Q: 运行时崩溃 "无法加载yolov8ncnn"
A: 确保NDK已正确配置，并且CMake已正确编译native库。检查 `app/build/intermediates/cmake/` 目录中是否有生成的 `.so` 文件。

### Q: 模型加载失败
A: 检查模型文件是否在 `app/src/main/assets/yolov8n_ncnn_model/` 目录中，文件名是否正确。

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── assets/
│   │   │   └── yolov8n_ncnn_model/      # 模型文件
│   │   ├── cpp/                         # C++/JNI代码
│   │   ├── java/                        # Kotlin/Java代码
│   │   └── res/                         # 资源文件
│   └── ncnn/                            # NCNN库（需要手动添加）
│       ├── include/
│       └── lib/
└── build.gradle.kts
```

## 下一步

完成设置后，请参考 [README.md](README.md) 了解如何使用应用。

