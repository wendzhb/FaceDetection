#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <android/log.h>

#define LOG_TAG "native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
extern "C" {

JNIEXPORT jstring
JNICALL
Java_com_kaifa_facedetection_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

using namespace cv;

CascadeClassifier *faceClassifier;

ANativeWindow *nativeWindow;

JNIEXPORT void JNICALL
Java_com_kaifa_facedetection_MainActivity_loadModel(JNIEnv *env, jobject instance, jstring path_) {

    const char *path = env->GetStringUTFChars(path_, 0);

    faceClassifier = new CascadeClassifier(path);

    env->ReleaseStringUTFChars(path_, path);
}

JNIEXPORT void JNICALL
Java_com_kaifa_facedetection_MainActivity_process(JNIEnv *env, jobject instance, jobject bitmap) {

    // TODO

}

//将surface --》 ANativeWindow
JNIEXPORT void JNICALL
Java_com_kaifa_facedetection_MainActivity_setSurface(JNIEnv *env, jobject instance, jobject surface,
                                                     jint width, jint heigh) {

    if (surface) {
        if (nativeWindow) {
            ANativeWindow_release(nativeWindow);
            nativeWindow = 0;
        }

        LOGI("new native window");
        nativeWindow = ANativeWindow_fromSurface(env, surface);

        if (nativeWindow) {
            //设置nativewindow的分辨率和显示图像的格式
            ANativeWindow_setBuffersGeometry(nativeWindow, width, heigh, WINDOW_FORMAT_RGBA_8888);
        }
    } else {
        if (nativeWindow) {
            LOGI("release old native window");

            ANativeWindow_release(nativeWindow);
            nativeWindow = 0;
        }
    }


}

}