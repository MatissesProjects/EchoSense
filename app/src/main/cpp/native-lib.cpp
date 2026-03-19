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

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setNoiseGateThreshold(JNIEnv *env, jobject /* this */, jfloat threshold) {
    if (audioEngine != nullptr) {
        audioEngine->setNoiseGateThreshold(threshold);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setEqualizerBandGain(JNIEnv *env, jobject /* this */, jint bandIndex, jfloat gain) {
    if (audioEngine != nullptr) {
        audioEngine->setEqualizerBandGain(bandIndex, gain);
    }
}