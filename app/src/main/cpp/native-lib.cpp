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
Java_com_echosense_app_MainActivity_setInputSource(JNIEnv *env, jobject /* this */, jint source) {
    if (audioEngine != nullptr) {
        audioEngine->setInputSource(static_cast<InputSource>(source));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setInputDevice(JNIEnv *env, jobject /* this */, jint deviceId) {
    if (audioEngine != nullptr) {
        audioEngine->setInputDevice(deviceId);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setRemoteGain(JNIEnv *env, jobject /* this */, jfloat gain) {
    if (audioEngine != nullptr) {
        audioEngine->setRemoteGain(gain);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_writeRemoteAudio(JNIEnv *env, jobject /* this */, jfloatArray data) {
    if (audioEngine != nullptr) {
        jfloat *c_data = env->GetFloatArrayElements(data, NULL);
        jsize len = env->GetArrayLength(data);
        audioEngine->writeRemoteAudio(c_data, len);
        env->ReleaseFloatArrayElements(data, c_data, JNI_ABORT);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setPreAmpGain(JNIEnv *env, jobject /* this */, jfloat gain) {
    if (audioEngine != nullptr) {
        audioEngine->setPreAmpGain(gain);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_MainActivity_setVoiceBoost(JNIEnv *env, jobject /* this */, jfloat gainDb) {
    if (audioEngine != nullptr) {
        audioEngine->setVoiceBoost(gainDb);
    }
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
Java_com_echosense_app_MainActivity_setProfile(JNIEnv *env, jobject /* this */, jint profile) {
    if (audioEngine != nullptr) {
        audioEngine->setProfile(static_cast<AudioProfile>(profile));
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

// Service JNI exports
extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_EchoSenseService_startAudioEngine(JNIEnv *env, jobject /* this */) {
    if (audioEngine == nullptr) {
        audioEngine = new AudioEngine();
    }
    audioEngine->start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_EchoSenseService_stopAudioEngine(JNIEnv *env, jobject /* this */) {
    if (audioEngine != nullptr) {
        audioEngine->stop();
        delete audioEngine;
        audioEngine = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_echosense_app_EchoSenseService_writeRemoteAudio(JNIEnv *env, jobject /* this */, jfloatArray data) {
    if (audioEngine != nullptr) {
        jfloat *c_data = env->GetFloatArrayElements(data, NULL);
        jsize len = env->GetArrayLength(data);
        audioEngine->writeRemoteAudio(c_data, len);
        env->ReleaseFloatArrayElements(data, c_data, JNI_ABORT);
    }
}