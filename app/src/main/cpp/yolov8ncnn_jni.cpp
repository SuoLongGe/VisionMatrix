#include <jni.h>
#include <string>
#include <vector>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ncnn/ncnn.h>
#include "yolov8.h"

using Object = Yolov8::Object;

#define TAG "Yolov8NCNN"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static Yolov8* g_yolov8 = 0;
static ncnn::Mutex lock;

extern "C" {

JNIEXPORT jint JNI_ONLOAD(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnLoad");
    ncnn::create_gpu_instance();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_ONUNLOAD(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnUnload");
    ncnn::destroy_gpu_instance();
}

JNIEXPORT jint JNICALL
Java_com_tencent_ncnn_Yolov8_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jstring paramPath, jstring binPath) {
    if (g_yolov8) {
        delete g_yolov8;
        g_yolov8 = 0;
    }

    const char* param_path = env->GetStringUTFChars(paramPath, 0);
    const char* bin_path = env->GetStringUTFChars(binPath, 0);

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    LOGD("loadModel %s %s", param_path, bin_path);

    g_yolov8 = new Yolov8;
    g_yolov8->load(mgr, param_path, bin_path);

    env->ReleaseStringUTFChars(paramPath, param_path);
    env->ReleaseStringUTFChars(binPath, bin_path);

    return 0;
}

JNIEXPORT jobjectArray JNICALL
Java_com_tencent_ncnn_Yolov8_detect(JNIEnv* env, jobject thiz, jobject bitmap, jfloat threshold) {
    if (!g_yolov8) {
        LOGE("model not loaded");
        return nullptr;
    }

    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("bitmap format not RGBA_8888");
        return nullptr;
    }

    void* indata;
    AndroidBitmap_lockPixels(env, bitmap, &indata);

    ncnn::Mat in = ncnn::Mat::from_pixels((unsigned char*)indata, ncnn::Mat::PIXEL_RGBA, info.width, info.height);

    AndroidBitmap_unlockPixels(env, bitmap);

    std::vector<Object> objects;
    g_yolov8->detect(in, objects, threshold);

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

