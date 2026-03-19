#include <jni.h>
#include <string>
#include "AudioEngine.h"

static AudioEngine *audioEngine = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_startAudioEngine(JNIEnv *env, jobject /* this */) {
    if (audioEngine == nullptr) {
        audioEngine = new AudioEngine();
    }
    audioEngine->start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_stopAudioEngine(JNIEnv *env, jobject /* this */) {
    if (audioEngine != nullptr) {
        audioEngine->stop();
        delete audioEngine;
        audioEngine = nullptr;
    }
}