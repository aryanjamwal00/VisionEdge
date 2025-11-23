#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>

#define LOG_TAG "VisionEdgeNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace cv;

// ----------------------
// Simple init string (optional)
// ----------------------
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_visionedge_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("VisionEdge native module initialized successfully!");
    return env->NewStringUTF("VisionEdge native module initialized");
}

// ----------------------
// Frame processing via OpenCV
// ----------------------
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_visionedge_MainActivity_processFrameJNI(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray frameData,
        jint width,
        jint height) {

    jsize length = env->GetArrayLength(frameData);
    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);

    LOGI("Received frame from Kotlin: %dx%d, bytes=%d", width, height, length);

    // Wrap incoming bytes as ARGB_8888 image (4 channels)
    Mat rgba(height, width, CV_8UC4, (unsigned char*)dataPtr);

    // Convert to gray and apply Canny edge detection
    Mat gray, edges;
    cvtColor(rgba, gray, COLOR_RGBA2GRAY);
    Canny(gray, edges, 80, 150);

    // Convert edges (1 channel) back to RGBA (4 channels)
    Mat edgesRgba;
    cvtColor(edges, edgesRgba, COLOR_GRAY2RGBA);

    // Prepare output byte array (same size: width * height * 4)
    int outSize = width * height * 4;
    jbyteArray outArray = env->NewByteArray(outSize);
    env->SetByteArrayRegion(outArray, 0, outSize, (jbyte*)edgesRgba.data);

    env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);

    LOGI("Returning processed frame to Kotlin");
    return outArray;
}
