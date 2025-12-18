cmake_minimum_required(VERSION 3.18.1)
project("yolov8ncnn")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 开启 OpenMP 支持
find_package(OpenMP REQUIRED)
if(OPENMP_FOUND)
    message("OpenMP FOUND")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")
endif()

# NCNN路径
set(NCNN_DIR ${CMAKE_SOURCE_DIR}/../../../ncnn)
set(NCNN_INCLUDE_DIR ${NCNN_DIR}/include)
set(NCNN_LIB_DIR ${NCNN_DIR}/lib)

# 包含目录
include_directories(${NCNN_INCLUDE_DIR})

# 辅助宏：添加静态库
macro(add_static_lib name)
    add_library(${name} STATIC IMPORTED)
    set_target_properties(${name} PROPERTIES IMPORTED_LOCATION ${NCNN_LIB_DIR}/${ANDROID_ABI}/lib${name}.a)
endmacro()

# 添加 ncnn 及其依赖的 vulkan 支持库
add_static_lib(ncnn)
add_static_lib(glslang)
add_static_lib(SPIRV)
add_static_lib(OSDependent)
add_static_lib(MachineIndependent)
add_static_lib(GenericCodeGen)
add_static_lib(glslang-default-resource-limits)

# 源文件
add_library(yolov8ncnn SHARED
    yolov8ncnn_jni.cpp
    yolov8.cpp
)

# 链接库
target_link_libraries(yolov8ncnn
    ncnn
    glslang
    SPIRV
    OSDependent
    MachineIndependent
    GenericCodeGen
    glslang-default-resource-limits
    OpenMP::OpenMP_CXX
    jnigraphics
    android
    log
)
