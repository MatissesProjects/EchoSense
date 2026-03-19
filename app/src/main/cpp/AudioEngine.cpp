#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mBandGains[i].store(0.0f);
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
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setUsage(oboe::Usage::VoiceCommunication);
    builder.setContentType(oboe::ContentType::Speech);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);

    if (builder.openStream(&mPlaybackStream) != oboe::Result::OK) return false;
    mSampleRate = (float) mPlaybackStream->getSampleRate();

    builder.setDirection(oboe::Direction::Input);
    builder.setInputPreset(oboe::InputPreset::Unprocessed);
    builder.setDataCallback(this);
    builder.setSampleRate((int32_t)mSampleRate);

    if (builder.openStream(&mRecordingStream) != oboe::Result::OK) {
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
    if (mRecordingStream) mRecordingStream->close();
    if (mPlaybackStream) mPlaybackStream->close();
    mRecordingStream = nullptr;
    mPlaybackStream = nullptr;
    mIsRunning.store(false);
}

void AudioEngine::setPreAmpGain(float gain) { mPreAmpGain.store(gain); }
void AudioEngine::setVoiceBoost(float gainDb) { mVoiceBoostDb.store(gainDb); mParamsChanged.store(true); }
void AudioEngine::setNoiseGateThreshold(float threshold) { mNoiseGateThreshold.store(threshold); }
void AudioEngine::setMasterGain(float gain) { mMasterGain.store(gain); }
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
    // Dedicated Voice Band-Pass (300Hz - 3.5kHz)
    mVoiceFilters[0].setHighPass(300.0f, mSampleRate, 0.707f);
    mVoiceFilters[1].setLowPass(3500.0f, mSampleRate, 0.707f);
    mParamsChanged.store(false);
}

float AudioEngine::processSample(float sample) {
    // 1. Pre-Amp (Sensitivity)
    float out = sample * mPreAmpGain.load();

    // 2. Noise Gate
    if (std::abs(out) < mNoiseGateThreshold.load()) return 0.0f;

    // 3. Fixed High-Pass
    out = mHighPass.process(out);

    // 4. Parallel Voice Boost Path
    float voicePath = out;
    voicePath = mVoiceFilters[0].process(voicePath);
    voicePath = mVoiceFilters[1].process(voicePath);
    float voiceGain = powf(10.0f, mVoiceBoostDb.load() / 20.0f);
    
    // 5. EQ Path
    float eqPath = out;
    for (int i = 0; i < 5; ++i) {
        eqPath = mEQBands[i].process(eqPath);
    }

    // 6. Combine Paths
    out = eqPath + (voicePath * voiceGain);

    // 7. Master Gain & Limiter
    out *= mMasterGain.load();
    return std::clamp(out, -1.0f, 1.0f);
}

void AudioEngine::calculateVolume(const float* data, int numFrames) {
    float sumSq = 0;
    for (int i = 0; i < numFrames; ++i) sumSq += data[i] * data[i];
    mCurrentVolume.store(std::sqrt(sumSq / numFrames));
}

void AudioEngine::processFft(const float* data, int numFrames) {
    std::lock_guard<std::mutex> lock(mFftMutex);
    for (int i = 0; i < numFrames; ++i) {
        mFftBuffer[mFftWritePos] = data[i];
        mFftWritePos = (mFftWritePos + 1) % FFT_SIZE;
    }
    if (mFftWritePos % 512 == 0) {
        for (int i = 0; i < (int)mFftOutput.size(); ++i) {
            float sum = 0;
            for(int j=0; j<FFT_SIZE; j++) {
                sum += mFftBuffer[j] * sinf(2.0f * M_PI * (float)i * (float)j / (float)FFT_SIZE);
            }
            mFftOutput[i] = std::abs(sum) / (FFT_SIZE / 2);
        }
    }
}

void AudioEngine::getFftData(float* output, int size) {
    std::lock_guard<std::mutex> lock(mFftMutex);
    std::copy(mFftOutput.begin(), mFftOutput.begin() + std::min((int)mFftOutput.size(), size), output);
}

void AudioEngine::getEqCurveData(float* output, int size) {
    float voiceGain = powf(10.0f, mVoiceBoostDb.load() / 20.0f);
    for (int i = 0; i < size; ++i) {
        float freq = 20.0f * powf(1000.0f, (float)i / (float)size);
        float eqMag = mHighPass.getMagnitude(freq, mSampleRate);
        for (int b = 0; b < 5; ++b) eqMag *= mEQBands[b].getMagnitude(freq, mSampleRate);
        
        float vMag = mHighPass.getMagnitude(freq, mSampleRate);
        vMag *= mVoiceFilters[0].getMagnitude(freq, mSampleRate);
        vMag *= mVoiceFilters[1].getMagnitude(freq, mSampleRate);
        vMag *= voiceGain;
        
        output[i] = eqMag + vMag;
    }
}

void AudioEngine::autoTune() {
    mBandGains[0].store(-10.0f);
    mBandGains[1].store(-5.0f);
    mBandGains[2].store(5.0f);
    mBandGains[3].store(8.0f);
    mVoiceBoostDb.store(10.0f);
    mParamsChanged.store(true);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *floatData = static_cast<float *>(audioData);
    if (mParamsChanged.load()) updateFilters();
    for (int i = 0; i < numFrames; ++i) floatData[i] = processSample(floatData[i]);
    calculateVolume(floatData, numFrames);
    processFft(floatData, numFrames);
    if (mPlaybackStream) mPlaybackStream->write(floatData, numFrames, 1000000);
    return oboe::DataCallbackResult::Continue;
}