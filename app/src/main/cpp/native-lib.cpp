#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>

#define LOG_TAG "VisionEdgeNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;

// Optional simple string function (not strictly required)
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_visionedge_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("VisionEdge native module initialized successfully!");
    return env->NewStringUTF("VisionEdge native module initialized");
}

// mode: 0 = original, 1 = gray, 2 = edge
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_visionedge_MainActivity_processFrameJNI(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray frameData,
        jint width,
        jint height,
        jint mode) {

    jsize length = env->GetArrayLength(frameData);
    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);

    LOGI("Received frame from Kotlin: %dx%d, bytes=%d, mode=%d",
         width, height, length, mode);

    if (length < width * height * 4) {
        LOGE("Input buffer too small for ARGB_8888 image");
        env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);
        return nullptr;
    }

    // Wrap incoming bytes as ARGB_8888 image (4 channels)
    Mat rgba(height, width, CV_8UC4, (unsigned char*)dataPtr);
    Mat output;

    if (mode == 1) {
        // Grayscale
        Mat gray;
        cvtColor(rgba, gray, COLOR_RGBA2GRAY);
        cvtColor(gray, output, COLOR_GRAY2RGBA);
    } else if (mode == 2) {
        // Canny edges
        Mat gray, edges;
        cvtColor(rgba, gray, COLOR_RGBA2GRAY);
        Canny(gray, edges, 80, 150);
        cvtColor(edges, output, COLOR_GRAY2RGBA);
    } else {
        // Original
        output = rgba.clone();
    }

    // Prepare output byte array (width * height * 4)
    int outSize = width * height * 4;
    jbyteArray outArray = env->NewByteArray(outSize);
    env->SetByteArrayRegion(outArray, 0, outSize, (jbyte*)output.data);

    env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);

    LOGI("Returning processed frame to Kotlin");
    return outArray;
}
