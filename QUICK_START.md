# 快速开始指南

## 5分钟快速设置

### 步骤1：下载NCNN库（2分钟）

1. 访问 https://github.com/Tencent/ncnn/releases
2. 找到最新的release版本
3. **下载 `ncnn-android-vulkan.zip`**（推荐，支持GPU加速）
   - 如果设备不支持Vulkan或遇到问题，可以下载 `ncnn-android.zip`（基础版本）

### 步骤2：解压和复制（1分钟）

**Windows PowerShell:**
```powershell
# 假设下载的文件在Downloads目录
cd $env:USERPROFILE\Downloads
Expand-Archive -Path ncnn-android-vulkan.zip -DestinationPath . -Force

# 复制到项目目录
Copy-Item -Path ncnn-android-vulkan\include -Destination ..\RuanChuang\app\ncnn\include -Recurse -Force
Copy-Item -Path ncnn-android-vulkan\lib -Destination ..\RuanChuang\app\ncnn\lib -Recurse -Force
```

**或者手动操作：**
1. 解压 `ncnn-android-vulkan.zip`
2. 将解压后的 `include` 文件夹复制到 `app/ncnn/include`
3. 将解压后的 `lib` 文件夹复制到 `app/ncnn/lib`

### 步骤3：检查目录结构（30秒）

确保目录结构如下：
```
app/
└── ncnn/
    ├── include/
    │   └── ncnn/
    │       ├── allocator.h
    │       ├── net.h
    │       └── ... (其他头文件)
    └── lib/
        ├── arm64-v8a/
        │   └── libncnn.a
        └── armeabi-v7a/
            └── libncnn.a
```

### 步骤4：配置Vulkan支持（如果使用vulkan版本）

编辑 `app/src/main/cpp/yolov8.cpp`，确保第34行：
```cpp
yolov8.opt.use_vulkan_compute = true;  // vulkan版本设为true
```

如果使用普通版本（`ncnn-android.zip`），改为：
```cpp
yolov8.opt.use_vulkan_compute = false;  // 普通版本设为false
```

### 步骤5：在Android Studio中打开（1分钟）

1. 打开Android Studio
2. File → Open → 选择项目根目录 `E:\RuanChuang`
3. 等待Gradle同步完成

### 步骤6：运行应用（30秒）

1. 连接Android设备（API 24+）或启动模拟器
2. 点击运行按钮（绿色三角形）
3. 首次编译可能需要5-10分钟（编译NDK代码）

## 验证安装

运行应用后：
1. 授予相机权限
2. 在搜索框输入"杯子"
3. 点击"搜索"按钮
4. 如果看到检测框，说明安装成功！

## 常见问题

### Q: 编译错误 "找不到ncnn.h"
A: 检查 `app/ncnn/include/ncnn/` 目录是否存在，确保头文件都在里面。

### Q: 链接错误 "找不到libncnn.a"
A: 检查 `app/ncnn/lib/arm64-v8a/libncnn.a` 和 `app/ncnn/lib/armeabi-v7a/libncnn.a` 是否存在。

### Q: 运行时崩溃 "无法加载yolov8ncnn"
A: 
- 检查NDK是否正确安装
- 查看logcat日志获取详细错误
- 确保CMake已正确编译native库

### Q: Vulkan相关错误
A: 
- 如果设备不支持Vulkan，下载 `ncnn-android.zip`（非vulkan版本）
- 将 `yolov8.cpp` 中的 `use_vulkan_compute` 设为 `false`

## 版本选择建议

| 版本 | 适用场景 | 性能 |
|------|---------|------|
| `ncnn-android-vulkan.zip` | 设备支持Vulkan，追求最佳性能 | ⭐⭐⭐⭐⭐ |
| `ncnn-android.zip` | 设备不支持Vulkan，或遇到兼容性问题 | ⭐⭐⭐ |

**推荐：优先尝试vulkan版本，如果遇到问题再使用普通版本。**


