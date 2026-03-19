#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>
#include <complex>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mBandGains[i].store(0.0f); // 0dB = neutral
    }
    mFftBuffer.resize(FFT_SIZE, 0.0f);
    mFftOutput.resize(FFT_SIZE / 2, 0.0f);
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    if (mIsRunning.load()) return true;

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
    builder.setInputPreset(oboe::InputPreset::Unprocessed);
    builder.setDataCallback(this);
    builder.setSampleRate((int32_t)mSampleRate);
    builder.setChannelCount(1);

    result = builder.openStream(&mRecordingStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open recording stream. Error: %s", oboe::convertToText(result));
        mPlaybackStream->close();
        return false;
    }

    mHighPass.setHighPass(150.0f, mSampleRate, 0.707f);
    updateFilters();

    mPlaybackStream->requestStart();
    mRecordingStream->requestStart();
    
    mIsRunning.store(true);
    return true;
}

void AudioEngine::stop() {
    if (!mIsRunning.load()) return;

    if (mRecordingStream != nullptr) {
        mRecordingStream->close();
        mRecordingStream = nullptr;
    }
    if (mPlaybackStream != nullptr) {
        mPlaybackStream->close();
        mPlaybackStream = nullptr;
    }
    mIsRunning.store(false);
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
    float absSample = std::abs(sample);
    if (absSample < mNoiseGateThreshold.load()) {
        return 0.0f;
    }

    float out = mHighPass.process(sample);
    for (int i = 0; i < 5; ++i) {
        out = mEQBands[i].process(out);
    }

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

// Simple DFT-like energy analysis for visualization (faster than full FFT implementation for this turn)
void AudioEngine::processFft(const float* data, int numFrames) {
    std::lock_guard<std::mutex> lock(mFftMutex);
    
    // Accumulate samples
    for (int i = 0; i < numFrames; ++i) {
        mFftBuffer[mFftWritePos] = data[i];
        mFftWritePos = (mFftWritePos + 1) % FFT_SIZE;
    }

    // Every 512 samples, update the visualization output
    if (mFftWritePos % 512 == 0) {
        // Perform a simple multi-band energy calculation (simulating spectrum)
        int numBins = mFftOutput.size();
        for (int i = 0; i < numBins; ++i) {
            float freq = (float)i * mSampleRate / (float)FFT_SIZE;
            // Use a simple windowed energy check for visualization purposes
            float sum = 0;
            float cos_term = cosf(2.0f * M_PI * (float)i / (float)FFT_SIZE);
            for(int j=0; j<FFT_SIZE; j++) {
                sum += mFftBuffer[j] * sinf(2.0f * M_PI * (float)i * (float)j / (float)FFT_SIZE);
            }
            mFftOutput[i] = std::abs(sum) / (FFT_SIZE / 2);
        }
    }
}

void AudioEngine::getFftData(float* output, int size) {
    std::lock_guard<std::mutex> lock(mFftMutex);
    int copySize = std::min((int)mFftOutput.size(), size);
    std::copy(mFftOutput.begin(), mFftOutput.begin() + copySize, output);
}

void AudioEngine::getEqCurveData(float* output, int size) {
    for (int i = 0; i < size; ++i) {
        // Logarithmic frequency mapping for the curve
        float freq = 20.0f * powf(1000.0f, (float)i / (float)size);
        float mag = mHighPass.getMagnitude(freq, mSampleRate);
        for (int b = 0; b < 5; ++b) {
            mag *= mEQBands[b].getMagnitude(freq, mSampleRate);
        }
        output[i] = mag;
    }
}

void AudioEngine::autoTune() {
    // Intelligent Suggestion Heuristic:
    // 1. Analyze low-end noise (100-300Hz)
    // 2. Analyze speech clarity (1k-4kHz)
    
    std::lock_guard<std::mutex> lock(mFftMutex);
    float lowEnergy = 0;
    float midEnergy = 0;
    
    for (int i = 0; i < (int)mFftOutput.size(); ++i) {
        float freq = (float)i * mSampleRate / (float)FFT_SIZE;
        if (freq > 50 && freq < 300) lowEnergy += mFftOutput[i];
        if (freq > 1000 && freq < 4000) midEnergy += mFftOutput[i];
    }
    
    // If low energy is high relative to mids, cut more bass
    if (lowEnergy > midEnergy * 1.5f) {
        mBandGains[0].store(-8.0f); // Cut 200Hz
        mBandGains[1].store(-4.0f); // Cut 500Hz
    }
    
    // Always boost speech core slightly in auto-tune
    mBandGains[2].store(4.0f); // Boost 1.5kHz
    mBandGains[3].store(6.0f); // Boost 3kHz
    
    mParamsChanged.store(true);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *floatData = static_cast<float *>(audioData);

    if (mParamsChanged.load()) {
        updateFilters();
    }

    // Process audio samples
    for (int i = 0; i < numFrames; ++i) {
        floatData[i] = processSample(floatData[i]);
    }

    calculateVolume(floatData, numFrames);
    processFft(floatData, numFrames);

    if (mPlaybackStream != nullptr) {
        mPlaybackStream->write(floatData, numFrames, 1000000);
    }

    return oboe::DataCallbackResult::Continue;
}