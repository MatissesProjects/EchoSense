#include <jni.h>
#include <string>
#include "AudioEngine.h"

static AudioEngine *audioEngine = nullptr;

#define JNI_METHOD(name) Java_com_echosense_app_AudioEngineLib_##name

extern "C" {

JNIEXPORT void JNICALL JNI_METHOD(startAudioEngine)(JNIEnv *env, jobject) {
    if (audioEngine == nullptr) audioEngine = new AudioEngine();
    audioEngine->start();
}

JNIEXPORT void JNICALL JNI_METHOD(stopAudioEngine)(JNIEnv *env, jobject) {
    if (audioEngine != nullptr) {
        audioEngine->stop();
        delete audioEngine;
        audioEngine = nullptr;
    }
}

JNIEXPORT jboolean JNICALL JNI_METHOD(isAudioEngineRunning)(JNIEnv *env, jobject) {
    return (audioEngine != nullptr && audioEngine->isRunning());
}

JNIEXPORT void JNICALL JNI_METHOD(setInputSource)(JNIEnv *env, jobject, jint source) {
    if (audioEngine != nullptr) audioEngine->setInputSource(static_cast<InputSource>(source));
}

JNIEXPORT void JNICALL JNI_METHOD(setInputDevice)(JNIEnv *env, jobject, jint deviceId) {
    if (audioEngine != nullptr) audioEngine->setInputDevice(deviceId);
}

JNIEXPORT void JNICALL JNI_METHOD(setRemoteGain)(JNIEnv *env, jobject, jfloat gain) {
    if (audioEngine != nullptr) audioEngine->setRemoteGain(gain);
}

JNIEXPORT void JNICALL JNI_METHOD(writeRemoteAudio)(JNIEnv *env, jobject, jfloatArray data) {
    if (audioEngine != nullptr) {
        jfloat *c_data = env->GetFloatArrayElements(data, NULL);
        jsize len = env->GetArrayLength(data);
        audioEngine->writeRemoteAudio(c_data, len);
        env->ReleaseFloatArrayElements(data, c_data, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL JNI_METHOD(setPreAmpGain)(JNIEnv *env, jobject, jfloat gain) {
    if (audioEngine != nullptr) audioEngine->setPreAmpGain(gain);
}

JNIEXPORT void JNICALL JNI_METHOD(setVoiceBoost)(JNIEnv *env, jobject, jfloat gainDb) {
    if (audioEngine != nullptr) audioEngine->setVoiceBoost(gainDb);
}

JNIEXPORT void JNICALL JNI_METHOD(setHpfFreq)(JNIEnv *env, jobject, jfloat freq) {
    if (audioEngine != nullptr) audioEngine->setHpfFreq(freq);
}

JNIEXPORT void JNICALL JNI_METHOD(setLpfFreq)(JNIEnv *env, jobject, jfloat freq) {
    if (audioEngine != nullptr) audioEngine->setLpfFreq(freq);
}

JNIEXPORT void JNICALL JNI_METHOD(setLimiterThreshold)(JNIEnv *env, jobject, jfloat threshold) {
    if (audioEngine != nullptr) audioEngine->setLimiterThreshold(threshold);
}

JNIEXPORT void JNICALL JNI_METHOD(setNoiseGateThreshold)(JNIEnv *env, jobject, jfloat threshold) {
    if (audioEngine != nullptr) audioEngine->setNoiseGateThreshold(threshold);
}

JNIEXPORT void JNICALL JNI_METHOD(setSpectralReduction)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setSpectralReduction(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setSpectralGateThreshold)(JNIEnv *env, jobject, jfloat threshold) {
    if (audioEngine != nullptr) audioEngine->setSpectralGateThreshold(threshold);
}

JNIEXPORT void JNICALL JNI_METHOD(setDereverbStrength)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setDereverbStrength(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setHpssStrength)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setHpssStrength(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setFreqWarpStrength)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setFreqWarpStrength(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setNeuralMaskStrength)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setNeuralMaskStrength(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setBassBoostStrength)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setBassBoostStrength(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(learnNoise)(JNIEnv *env, jobject) {
    if (audioEngine != nullptr) audioEngine->learnNoise();
}

JNIEXPORT void JNICALL JNI_METHOD(setMasterGain)(JNIEnv *env, jobject, jfloat gain) {
    if (audioEngine != nullptr) audioEngine->setMasterGain(gain);
}

JNIEXPORT void JNICALL JNI_METHOD(setProfile)(JNIEnv *env, jobject, jint profile) {
    if (audioEngine != nullptr) audioEngine->setProfile(static_cast<AudioProfile>(profile));
}

JNIEXPORT void JNICALL JNI_METHOD(setSensorFusion)(JNIEnv *env, jobject, jboolean enabled) {
    if (audioEngine != nullptr) audioEngine->setSensorFusion(enabled);
}

JNIEXPORT void JNICALL JNI_METHOD(setTargetLock)(JNIEnv *env, jobject, jboolean enabled) {
    if (audioEngine != nullptr) audioEngine->setTargetLock(enabled);
}

JNIEXPORT void JNICALL JNI_METHOD(setTargetSpeaker)(JNIEnv *env, jobject, jint speakerId) {
    if (audioEngine != nullptr) audioEngine->setTargetSpeaker(speakerId);
}

JNIEXPORT void JNICALL JNI_METHOD(setMbCompression)(JNIEnv *env, jobject, jfloat ratio) {
    if (audioEngine != nullptr) audioEngine->setMbCompression(ratio);
}

JNIEXPORT void JNICALL JNI_METHOD(setBeamforming)(JNIEnv *env, jobject, jboolean enabled) {
    if (audioEngine != nullptr) audioEngine->setBeamforming(enabled);
}

JNIEXPORT void JNICALL JNI_METHOD(setTransientSuppression)(JNIEnv *env, jobject, jfloat strength) {
    if (audioEngine != nullptr) audioEngine->setTransientSuppression(strength);
}

JNIEXPORT void JNICALL JNI_METHOD(setEqualizerBandGain)(JNIEnv *env, jobject, jint bandIndex, jfloat gain) {
    if (audioEngine != nullptr) audioEngine->setEqualizerBandGain(bandIndex, gain);
}

JNIEXPORT jfloat JNICALL JNI_METHOD(getVolumeLevel)(JNIEnv *env, jobject) {
    return (audioEngine != nullptr) ? audioEngine->getVolumeLevel() : 0.0f;
}

JNIEXPORT jfloat JNICALL JNI_METHOD(getIsolationGainDb)(JNIEnv *env, jobject) {
    return (audioEngine != nullptr) ? audioEngine->getIsolationGainDb() : 0.0f;
}

JNIEXPORT jint JNICALL JNI_METHOD(getDominantMic)(JNIEnv *env, jobject) {
    return (audioEngine != nullptr) ? audioEngine->getDominantMic() : 0;
}

JNIEXPORT void JNICALL JNI_METHOD(getFftData)(JNIEnv *env, jobject, jfloatArray output) {
    if (audioEngine != nullptr) {
        jfloat *c_output = env->GetFloatArrayElements(output, NULL);
        jsize len = env->GetArrayLength(output);
        audioEngine->getFftData(c_output, len);
        env->ReleaseFloatArrayElements(output, c_output, 0);
    }
}

JNIEXPORT jint JNICALL JNI_METHOD(getSpeakerInfo)(JNIEnv *env, jobject, jintArray ids, jfloatArray energyPhone, jfloatArray energyWatch, jbooleanArray active) {
    if (audioEngine == nullptr) return 0;
    
    jsize maxSpeakers = env->GetArrayLength(ids);
    auto* speakerInfos = new AudioEngine::SpeakerInfo[maxSpeakers];
    
    audioEngine->getSpeakerInfo(speakerInfos, maxSpeakers);
    
    jint count = 2; // For now fixed 2
    
    jint* c_ids = env->GetIntArrayElements(ids, NULL);
    jfloat* c_phone = env->GetFloatArrayElements(energyPhone, NULL);
    jfloat* c_watch = env->GetFloatArrayElements(energyWatch, NULL);
    jboolean* c_active = env->GetBooleanArrayElements(active, NULL);
    
    for (int i = 0; i < count; i++) {
        c_ids[i] = speakerInfos[i].id;
        c_phone[i] = speakerInfos[i].energyPhone;
        c_watch[i] = speakerInfos[i].energyWatch;
        c_active[i] = speakerInfos[i].isActive;
    }
    
    env->ReleaseIntArrayElements(ids, c_ids, 0);
    env->ReleaseFloatArrayElements(energyPhone, c_phone, 0);
    env->ReleaseFloatArrayElements(energyWatch, c_watch, 0);
    env->ReleaseBooleanArrayElements(active, c_active, 0);
    
    delete[] speakerInfos;
    return count;
}

JNIEXPORT void JNICALL JNI_METHOD(getEqCurveData)(JNIEnv *env, jobject, jfloatArray output) {
    if (audioEngine != nullptr) {
        jfloat *c_output = env->GetFloatArrayElements(output, NULL);
        jsize len = env->GetArrayLength(output);
        audioEngine->getEqCurveData(c_output, len);
        env->ReleaseFloatArrayElements(output, c_output, 0);
    }
}

JNIEXPORT void JNICALL JNI_METHOD(autoTune)(JNIEnv *env, jobject) {
    if (audioEngine != nullptr) audioEngine->autoTune();
}

// Keep Service compatibility for direct calls if still used
JNIEXPORT void JNICALL Java_com_echosense_app_EchoSenseService_startAudioEngine(JNIEnv *env, jobject) {
    if (audioEngine == nullptr) audioEngine = new AudioEngine();
    audioEngine->start();
}
JNIEXPORT void JNICALL Java_com_echosense_app_EchoSenseService_stopAudioEngine(JNIEnv *env, jobject) {
    if (audioEngine != nullptr) {
        audioEngine->stop();
        delete audioEngine;
        audioEngine = nullptr;
    }
}
JNIEXPORT void JNICALL Java_com_echosense_app_EchoSenseService_writeRemoteAudio(JNIEnv *env, jobject, jfloatArray data) {
    if (audioEngine != nullptr) {
        jfloat *c_data = env->GetFloatArrayElements(data, NULL);
        jsize len = env->GetArrayLength(data);
        audioEngine->writeRemoteAudio(c_data, len);
        env->ReleaseFloatArrayElements(data, c_data, JNI_ABORT);
    }
}

} // extern "C"
