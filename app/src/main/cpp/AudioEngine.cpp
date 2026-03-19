#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mBandGains[i].store(1.0f);
    }
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;

    // --- 1. Setup Playback Stream ---
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);

    oboe::Result result = builder.openStream(&mPlaybackStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open playback stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    // --- 2. Setup Recording Stream ---
    builder.setDirection(oboe::Direction::Input);
    builder.setDataCallback(this);

    result = builder.openStream(&mRecordingStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open recording stream. Error: %s", oboe::convertToText(result));
        mPlaybackStream->close();
        return false;
    }

    // Start playback first, then recording
    mPlaybackStream->requestStart();
    mRecordingStream->requestStart();

    return true;
}

void AudioEngine::stop() {
    if (mRecordingStream != nullptr) {
        mRecordingStream->close();
        mRecordingStream = nullptr;
    }
    if (mPlaybackStream != nullptr) {
        mPlaybackStream->close();
        mPlaybackStream = nullptr;
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
    float absSample = std::abs(sample);
    if (absSample < mNoiseGateThreshold.load()) {
        return 0.0f;
    }
    return sample * mBandGains[0].load();
}

void AudioEngine::calculateVolume(const float* data, int numFrames) {
    float sumSq = 0;
    for (int i = 0; i < numFrames; ++i) {
        sumSq += data[i] * data[i];
    }
    float rms = std::sqrt(sumSq / numFrames);
    mCurrentVolume.store(rms);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *floatData = static_cast<float *>(audioData);

    // 1. Process audio samples in-place
    for (int i = 0; i < numFrames; ++i) {
        floatData[i] = processSample(floatData[i]);
    }

    // 2. Calculate volume for UI visualizer
    calculateVolume(floatData, numFrames);

    // 3. Write processed data to the playback stream
    if (mPlaybackStream != nullptr) {
        mPlaybackStream->write(floatData, numFrames, 0); // Write with zero timeout to avoid blocking
    }

    return oboe::DataCallbackResult::Continue;
}