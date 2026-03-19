#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mBandGains[i].store(1.0f); // Default gain 1.0 (no change)
    }
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);
    builder.setDataCallback(this);

    oboe::Result result = builder.openStream(&mStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to start stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    return true;
}

void AudioEngine::stop() {
    if (mStream != nullptr) {
        mStream->close();
        mStream = nullptr;
    }
}

void AudioEngine::setNoiseGateThreshold(float threshold) {
    mNoiseGateThreshold.store(threshold);
}

void AudioEngine::setEqualizerBandGain(int bandIndex, float gain) {
    if (bandIndex >= 0 && bandIndex < 5) {
        mBandGains[bandIndex].store(gain);
    }
}

float AudioEngine::processSample(float sample) {
    // POC: Simple Noise Gate
    float absSample = std::abs(sample);
    if (absSample < mNoiseGateThreshold.load()) {
        return 0.0f;
    }

    // POC: Simple Volume Boost (using first band gain as master volume for now)
    return sample * mBandGains[0].load();
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *floatData = static_cast<float *>(audioData);
    for (int i = 0; i < numFrames; ++i) {
        floatData[i] = processSample(floatData[i]);
    }
    return oboe::DataCallbackResult::Continue;
}