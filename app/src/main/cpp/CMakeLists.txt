cmake_minimum_required(VERSION 3.18.1)
project("yolov8ncnn")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# NCNN路径（需要根据实际路径修改）
# 默认路径：app/ncnn/
# 如果NCNN库在其他位置，请修改以下路径
set(NCNN_DIR ${CMAKE_SOURCE_DIR}/../../../ncnn)
set(NCNN_INCLUDE_DIR ${NCNN_DIR}/include)
set(NCNN_LIB_DIR ${NCNN_DIR}/lib)

# 如果NCNN库在项目根目录下的ncnn文件夹中，使用以下路径：
# set(NCNN_INCLUDE_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn/include)
# set(NCNN_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../ncnn/lib)

# 检查NCNN库文件是否存在
set(NCNN_LIB_PATH ${NCNN_LIB_DIR}/${ANDROID_ABI}/libncnn.a)
if(NOT EXISTS ${NCNN_LIB_PATH})
    message(FATAL_ERROR "
    ============================================
    ❌ NCNN库文件未找到！
    ============================================
    
    找不到文件: ${NCNN_LIB_PATH}
    
    请按照以下步骤设置NCNN库：
    
    1. 访问 https://github.com/Tencent/ncnn/releases
    2. 下载 ncnn-android-vulkan.zip 或 ncnn-android.zip
    3. 解压后将 include 和 lib 目录复制到 app/ncnn/
    
    详细说明请查看: app/ncnn/README_NCNN_SETUP.md
    或项目根目录的 SETUP.md 文件
    
    ============================================
    ")
endif()

# 检查头文件目录
if(NOT EXISTS ${NCNN_INCLUDE_DIR})
    message(FATAL_ERROR "
    ============================================
    ❌ NCNN头文件目录未找到！
    ============================================
    
    找不到目录: ${NCNN_INCLUDE_DIR}
    
    请确保已正确复制NCNN库的include目录到 app/ncnn/include/
    
    ============================================
    ")
endif()

# 添加NCNN库
add_library(ncnn STATIC IMPORTED)
set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${NCNN_LIB_PATH})

# 包含目录
include_directories(${NCNN_INCLUDE_DIR})

# 源文件
add_library(yolov8ncnn SHARED
    yolov8ncnn_jni.cpp
    yolov8.cpp
)

# 链接库
target_link_libraries(yolov8ncnn
    ncnn
    jnigraphics
    android
    log
)

