#include <android/log.h>

#define TAG "NewFeatureProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// Placeholder processor for new feature.
int processNewFeature(int input) {
    LOGD("processNewFeature called with input=%d", input);
    return input;
}

