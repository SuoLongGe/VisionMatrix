#include <jni.h>
#include <android/log.h>

#define TAG "NewFeatureJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" jint
Java_com_visionmatrix_ctrlf_newfeature_NativeBridge_process(JNIEnv* env, jobject /*thiz*/, jint value) {
    LOGD("New feature JNI placeholder called, value=%d", value);
    return value;
}

