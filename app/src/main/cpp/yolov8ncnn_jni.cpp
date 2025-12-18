#include <jni.h>
#include <string>
#include <vector>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <ncnn/net.h>
#include <ncnn/gpu.h>
#include <ncnn/cpu.h>
#include <ncnn/mat.h>
#include <ncnn/platform.h>

#include "yolov8.h"

using Object = ::Object;

#define TAG "Yolov8NCNN"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static Yolov8* g_yolov8 = 0;
static ncnn::Mutex lock;

extern "C" {

JNIEXPORT jint JNI_ONLOAD(JavaVM* vm, void* reserved) {
    ncnn::create_gpu_instance();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_ONUNLOAD(JavaVM* vm, void* reserved) {
    ncnn::destroy_gpu_instance();
}

JNIEXPORT jint JNICALL
Java_com_tencent_ncnn_Yolov8_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jstring paramPath, jstring binPath) {
    ncnn::MutexLockGuard g(lock);
    if (g_yolov8) {
        delete g_yolov8;
        g_yolov8 = 0;
    }
    const char* param_path = env->GetStringUTFChars(paramPath, 0);
    const char* bin_path = env->GetStringUTFChars(binPath, 0);
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    g_yolov8 = new Yolov8;
    g_yolov8->load(mgr, param_path, bin_path);
    env->ReleaseStringUTFChars(paramPath, param_path);
    env->ReleaseStringUTFChars(binPath, bin_path);
    return 0;
}

JNIEXPORT jobjectArray JNICALL
Java_com_tencent_ncnn_Yolov8_detect(JNIEnv* env, jobject thiz, jobject bitmap, jfloat threshold) {
    ncnn::MutexLockGuard g(lock);
    if (!g_yolov8) return nullptr;

    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    
    void* indata;
    AndroidBitmap_lockPixels(env, bitmap, &indata);

    // 此时 in 只是引用了 indata 的指针，并未拷贝
    ncnn::Mat in = ncnn::Mat::from_pixels((unsigned char*)indata, ncnn::Mat::PIXEL_RGBA, info.width, info.height);

    std::vector<Object> objects;
    // 必须在 unlock 之前调用，确保内存有效
    g_yolov8->detect(in, objects, threshold);

    AndroidBitmap_unlockPixels(env, bitmap);

    jclass resultClass = env->FindClass("com/tencent/ncnn/Yolov8$DetectionResult");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "()V");
    jfieldID classIdField = env->GetFieldID(resultClass, "classId", "I");
    jfieldID classNameField = env->GetFieldID(resultClass, "className", "Ljava/lang/String;");
    jfieldID confidenceField = env->GetFieldID(resultClass, "confidence", "F");
    jfieldID xField = env->GetFieldID(resultClass, "x", "F");
    jfieldID yField = env->GetFieldID(resultClass, "y", "F");
    jfieldID widthField = env->GetFieldID(resultClass, "width", "F");
    jfieldID heightField = env->GetFieldID(resultClass, "height", "F");

    jobjectArray resultArray = env->NewObjectArray(objects.size(), resultClass, nullptr);
    for (size_t i = 0; i < objects.size(); i++) {
        jobject result = env->NewObject(resultClass, resultConstructor);
        env->SetIntField(result, classIdField, objects[i].label);
        env->SetObjectField(result, classNameField, env->NewStringUTF(g_yolov8->get_class_name(objects[i].label).c_str()));
        env->SetFloatField(result, confidenceField, objects[i].prob);
        env->SetFloatField(result, xField, objects[i].rect.x);
        env->SetFloatField(result, yField, objects[i].rect.y);
        env->SetFloatField(result, widthField, objects[i].rect.width);
        env->SetFloatField(result, heightField, objects[i].rect.height);
        env->SetObjectArrayElement(resultArray, i, result);
    }
    return resultArray;
}
}
