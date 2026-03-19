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

extern "C" JNIEXPORT jboolean JNICALL
Java_com_echosense_app_MainActivity_isAudioEngineRunning(JNIEnv *env, jobject /* this */) {
    if (audioEngine != nullptr) {
        return (jboolean) audioEngine->isRunning();
    }
    return false;
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setNoiseGateThreshold(JNIEnv *env, jobject /* this */, jfloat threshold) {
    if (audioEngine != nullptr) {
        audioEngine->setNoiseGateThreshold(threshold);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setMasterGain(JNIEnv *env, jobject /* this */, jfloat gain) {
    if (audioEngine != nullptr) {
        audioEngine->setMasterGain(gain);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setEqualizerBandGain(JNIEnv *env, jobject /* this */, jint bandIndex, jfloat gain) {
    if (audioEngine != nullptr) {
        audioEngine->setEqualizerBandGain(bandIndex, gain);
    }
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_echosense_app_MainActivity_getVolumeLevel(JNIEnv *env, jobject /* this */) {
    if (audioEngine != nullptr) {
        return (jfloat) audioEngine->getVolumeLevel();
    }
    return 0.0f;
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_getFftData(JNIEnv *env, jobject /* this */, jfloatArray output) {
    if (audioEngine != nullptr) {
        jfloat *c_output = env->GetFloatArrayElements(output, NULL);
        jsize len = env->GetArrayLength(output);
        audioEngine->getFftData(c_output, len);
        env->ReleaseFloatArrayElements(output, c_output, 0);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_getEqCurveData(JNIEnv *env, jobject /* this */, jfloatArray output) {
    if (audioEngine != nullptr) {
        jfloat *c_output = env->GetFloatArrayElements(output, NULL);
        jsize len = env->GetArrayLength(output);
        audioEngine->getEqCurveData(c_output, len);
        env->ReleaseFloatArrayElements(output, c_output, 0);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_autoTune(JNIEnv *env, jobject /* this */) {
    if (audioEngine != nullptr) {
        audioEngine->autoTune();
    }
}