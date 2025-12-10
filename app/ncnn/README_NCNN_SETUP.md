# ⚠️ NCNN库缺失 - 请先设置NCNN库

## 错误说明

编译失败是因为NCNN库文件还没有下载和放置。

## 快速解决方案

### 步骤1：下载NCNN库

1. 访问：https://github.com/Tencent/ncnn/releases
2. 下载：**`ncnn-android-vulkan.zip`**（推荐）或 `ncnn-android.zip`
3. 解压下载的zip文件

### 步骤2：复制文件到项目

解压后，将以下内容复制到当前目录（`app/ncnn/`）：

```
ncnn-android-vulkan/
├── include/          → 复制到 app/ncnn/include/
└── lib/             → 复制到 app/ncnn/lib/
```

**Windows PowerShell命令：**
```powershell
# 假设解压后的文件夹在Downloads目录
$ncnnPath = "$env:USERPROFILE\Downloads\ncnn-android-vulkan"

# 复制include目录
Copy-Item -Path "$ncnnPath\include" -Destination ".\include" -Recurse -Force

# 复制lib目录
Copy-Item -Path "$ncnnPath\lib" -Destination ".\lib" -Recurse -Force
```

**或者手动操作：**
1. 打开解压后的 `ncnn-android-vulkan` 文件夹
2. 将 `include` 文件夹复制到 `app/ncnn/include`
3. 将 `lib` 文件夹复制到 `app/ncnn/lib`

### 步骤3：验证目录结构

完成后，目录结构应该是：

```
app/ncnn/
├── include/
│   └── ncnn/
│       ├── allocator.h
│       ├── net.h
│       └── ... (其他头文件)
└── lib/
    ├── arm64-v8a/
    │   └── libncnn.a    ← 这个文件必须存在！
    └── armeabi-v7a/
        └── libncnn.a    ← 这个文件必须存在！
```

### 步骤4：重新编译

在Android Studio中：
1. Build → Clean Project
2. Build → Rebuild Project

## 详细说明

请参考项目根目录的 `SETUP.md` 或 `QUICK_START.md` 文件获取更详细的说明。

