#include <android/log.h>
#include <jni.h>

#define LOG_TAG "VisionEdgeNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_visionedge_MainActivity_stringFromJNI(JNIEnv* env, jobject) {
    LOGI("VisionEdge native module initialized successfully!");
    return env->NewStringUTF("VisionEdge native module initialized");
}
