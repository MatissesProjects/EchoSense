#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mBandGains[i].store(0.0f); // 0dB = neutral
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
    builder.setUsage(oboe::Usage::VoiceCommunication);
    builder.setContentType(oboe::ContentType::Speech);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);

    oboe::Result result = builder.openStream(&mPlaybackStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open playback stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    mSampleRate = (float) mPlaybackStream->getSampleRate();

    // --- 2. Setup Recording Stream ---
    builder.setDirection(oboe::Direction::Input);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setInputPreset(oboe::InputPreset::Unprocessed); // RAW audio, no system-level suppression
    builder.setDataCallback(this);
    builder.setSampleRate((int32_t)mSampleRate); // Match playback sample rate
    builder.setChannelCount(1);

    result = builder.openStream(&mRecordingStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open recording stream. Error: %s", oboe::convertToText(result));
        // Try again without InputPreset::Unprocessed (fallback to VoiceRecognition)
        builder.setInputPreset(oboe::InputPreset::VoiceRecognition);
        result = builder.openStream(&mRecordingStream);
        if (result != oboe::Result::OK) {
            mPlaybackStream->close();
            return false;
        }
    }

    // Initialize High-Pass filter to cut fan rumble (150Hz)
    mHighPass.setHighPass(150.0f, mSampleRate, 0.707f);

    // Initial filter update
    updateFilters();

    // Start streams
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

void AudioEngine::setMasterGain(float gain) {
    mMasterGain.store(gain);
}

void AudioEngine::setEqualizerBandGain(int bandIndex, float gainDb) {
    if (bandIndex >= 0 && bandIndex < 5) {
        mBandGains[bandIndex].store(gainDb);
        mParamsChanged.store(true);
    }
}

void AudioEngine::updateFilters() {
    float freqs[5] = {200.0f, 500.0f, 1500.0f, 3000.0f, 6000.0f};
    for (int i = 0; i < 5; ++i) {
        mEQBands[i].setPeaking(freqs[i], mSampleRate, 1.0f, mBandGains[i].load());
    }
    mParamsChanged.store(false);
}

float AudioEngine::processSample(float sample) {
    // 1. Noise Gate (Pre-EQ)
    float absSample = std::abs(sample);
    if (absSample < mNoiseGateThreshold.load()) {
        return 0.0f;
    }

    // 2. High Pass Filter (Permanent Low-Cut for fan/rumble)
    float out = mHighPass.process(sample);

    // 3. 5-Band Equalizer (Cascaded Biquads)
    for (int i = 0; i < 5; ++i) {
        out = mEQBands[i].process(out);
    }

    // 4. Master Gain & Limiter
    out *= mMasterGain.load();
    if (out > 1.0f) out = 1.0f;
    if (out < -1.0f) out = -1.0f;

    return out;
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

    // Check if UI changed any EQ parameters
    if (mParamsChanged.load()) {
        updateFilters();
    }

    // 1. Process audio samples
    for (int i = 0; i < numFrames; ++i) {
        floatData[i] = processSample(floatData[i]);
    }

    // 2. Volume Meter
    calculateVolume(floatData, numFrames);

    // 3. Playback
    if (mPlaybackStream != nullptr) {
        // Write processed data to the playback stream
        // We use a small timeout to allow Oboe to handle small jitter
        mPlaybackStream->write(floatData, numFrames, 1000000); // 1ms timeout
    }

    return oboe::DataCallbackResult::Continue;
}