#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) mBandGains[i].store(0.0f);
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    if (mIsRunning.load()) return true;
    oboe::AudioStreamBuilder builder;

    mCurrentRampGain = 0.0f;
    mParamsChanged.store(true);

    // 1. Playback Stream
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setUsage(oboe::Usage::VoiceCommunication);
    builder.setContentType(oboe::ContentType::Speech);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);
    builder.setDataCallback(this);

    if (builder.openStream(&mPlaybackStream) != oboe::Result::OK) return false;
    mSampleRate = (float) mPlaybackStream->getSampleRate();

    // 2. Recording Stream
    oboe::AudioStreamBuilder recBuilder;
    recBuilder.setDirection(oboe::Direction::Input);
    recBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    recBuilder.setSharingMode(oboe::SharingMode::Shared);
    recBuilder.setInputPreset(oboe::InputPreset::VoiceRecognition);
    recBuilder.setDataCallback(nullptr);
    recBuilder.setSampleRate((int32_t)mSampleRate);
    recBuilder.setChannelCount(1);
    recBuilder.setFormat(oboe::AudioFormat::Float);
    
    int32_t deviceId = mInputDeviceId.load();
    if (deviceId != oboe::kUnspecified) {
        recBuilder.setDeviceId(deviceId);
    }

    if (recBuilder.openStream(&mRecordingStream) != oboe::Result::OK) {
        mPlaybackStream->close();
        return false;
    }

    mHighPass.setHighPass(150.0f, mSampleRate, 0.707f);
    updateFilters();

    mRecordingStream->requestStart();
    mPlaybackStream->requestStart();
    mIsRunning.store(true);
    return true;
}

void AudioEngine::stop() {
    if (!mIsRunning.load()) return;
    mIsRunning.store(false);
    if (mRecordingStream) {
        mRecordingStream->stop();
        mRecordingStream->close();
        mRecordingStream = nullptr;
    }
    if (mPlaybackStream) {
        mPlaybackStream->stop();
        mPlaybackStream->close();
        mPlaybackStream = nullptr;
    }
}

void AudioEngine::setInputSource(InputSource source) {
    mInputSource.store(source);
}

void AudioEngine::setInputDevice(int32_t deviceId) {
    mInputDeviceId.store(deviceId);
}

void AudioEngine::writeRemoteAudio(const float* data, int32_t numFrames) {
    for (int i = 0; i < numFrames; ++i) {
        int nextPos = (mRemoteWritePos + 1) % REMOTE_BUFFER_SIZE;
        if (nextPos == mRemoteReadPos) break;
        mRemoteBuffer[mRemoteWritePos] = data[i];
        mRemoteWritePos = nextPos;
    }
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
    for (int i = 0; i < 5; ++i) mEQBands[i].setPeaking(freqs[i], mSampleRate, 1.0f, mBandGains[i].load());
    mVoiceFilters[0].setHighPass(300.0f, mSampleRate, 0.707f);
    mVoiceFilters[1].setLowPass(3500.0f, mSampleRate, 0.707f);
    mParamsChanged.store(false);
}

inline float AudioEngine::processSample(float sample) {
    if (mCurrentRampGain < 1.0f) mCurrentRampGain += mRampStep;
    float out = sample * mPreAmpGain.load();
    if (std::abs(out) < mNoiseGateThreshold.load()) return 0.0f;
    out = mHighPass.process(out);
    float voicePath = out;
    voicePath = mVoiceFilters[0].process(voicePath);
    voicePath = mVoiceFilters[1].process(voicePath);
    float voiceGain = powf(10.0f, mVoiceBoostDb.load() / 20.0f);
    float eqPath = out;
    for (int i = 0; i < 5; ++i) eqPath = mEQBands[i].process(eqPath);
    out = eqPath + (voicePath * voiceGain);
    out *= mMasterGain.load() * mCurrentRampGain;
    return std::clamp(out, -1.0f, 1.0f);
}

void AudioEngine::updateVisualization(const float* data, int numFrames) {
    std::lock_guard<std::mutex> lock(mVisMutex);
    float sumSq = 0;
    for (int i = 0; i < numFrames; ++i) sumSq += data[i] * data[i];
    float rms = sqrtf(sumSq / (float)numFrames);
    mCurrentVolume.store(rms);
    for(int i = VIS_BINS-1; i > 0; --i) mVisData[i] = mVisData[i-1];
    mVisData[0] = rms;
}

void AudioEngine::getFftData(float* output, int size) {
    std::lock_guard<std::mutex> lock(mVisMutex);
    std::copy(mVisData, mVisData + std::min((int)VIS_BINS, size), output);
}

void AudioEngine::getEqCurveData(float* output, int size) {
    float voiceGain = powf(10.0f, mVoiceBoostDb.load() / 20.0f);
    for (int i = 0; i < size; ++i) {
        float freq = 20.0f * powf(1000.0f, (float)i / (float)size);
        float eqMag = mHighPass.getMagnitude(freq, mSampleRate);
        for (int b = 0; b < 5; ++b) eqMag *= mEQBands[b].getMagnitude(freq, mSampleRate);
        float vMag = mHighPass.getMagnitude(freq, mSampleRate) * mVoiceFilters[0].getMagnitude(freq, mSampleRate) * mVoiceFilters[1].getMagnitude(freq, mSampleRate) * voiceGain;
        output[i] = eqMag + vMag;
    }
}

void AudioEngine::autoTune() {
    mBandGains[0].store(-10.0f); mBandGains[1].store(-5.0f);
    mBandGains[2].store(5.0f); mBandGains[3].store(8.0f);
    mVoiceBoostDb.store(10.0f); mParamsChanged.store(true);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *outputBuffer = static_cast<float *>(audioData);
    if (mParamsChanged.load()) updateFilters();

    if (mInputSource.load() == InputSource::Watch) {
        for (int i = 0; i < numFrames; ++i) {
            if (mRemoteReadPos != mRemoteWritePos) {
                outputBuffer[i] = mRemoteBuffer[mRemoteReadPos];
                mRemoteReadPos = (mRemoteReadPos + 1) % REMOTE_BUFFER_SIZE;
            } else {
                outputBuffer[i] = 0.0f;
            }
        }
    } else {
        if (mRecordingStream) {
            auto result = mRecordingStream->read(outputBuffer, numFrames, 0);
            if (result) {
                int32_t framesRead = result.value();
                if (framesRead < numFrames) {
                    for (int i = framesRead; i < numFrames; ++i) outputBuffer[i] = 0.0f;
                }
            } else {
                for (int i = 0; i < numFrames; ++i) outputBuffer[i] = 0.0f;
            }
        } else {
            for (int i = 0; i < numFrames; ++i) outputBuffer[i] = 0.0f;
        }
    }

    for (int i = 0; i < numFrames; ++i) {
        outputBuffer[i] = processSample(outputBuffer[i]);
    }

    updateVisualization(outputBuffer, numFrames);
    return oboe::DataCallbackResult::Continue;
}