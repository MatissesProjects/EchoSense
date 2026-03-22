#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    for (int i = 0; i < 5; ++i) {
        mManualBandGains[i].store(0.0f);
        mProfileBandGains[i].store(0.0f);
    }
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    if (mIsRunning.load()) return true;
    mCurrentRampGain = 0.0f;
    mParamsChanged.store(true);

    oboe::AudioStreamBuilder builder;
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
    if (deviceId != oboe::kUnspecified) recBuilder.setDeviceId(deviceId);

    if (recBuilder.openStream(&mRecordingStream) != oboe::Result::OK) {
        mPlaybackStream->close();
        return false;
    }

    updateFilters();
    mRecordingStream->requestStart();
    mPlaybackStream->requestStart();
    mIsRunning.store(true);
    return true;
}

void AudioEngine::stop() {
    if (!mIsRunning.load()) return;
    mIsRunning.store(false);
    if (mRecordingStream) { mRecordingStream->stop(); mRecordingStream->close(); mRecordingStream = nullptr; }
    if (mPlaybackStream) { mPlaybackStream->stop(); mPlaybackStream->close(); mPlaybackStream = nullptr; }
}

void AudioEngine::setInputSource(InputSource source) { mInputSource.store(source); }
void AudioEngine::setInputDevice(int32_t deviceId) { mInputDeviceId.store(deviceId); }
void AudioEngine::setRemoteGain(float gain) { mRemoteGain.store(gain); }

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
void AudioEngine::setHpfFreq(float freq) { mHpfFreq.store(freq); mParamsChanged.store(true); }
void AudioEngine::setLpfFreq(float freq) { mLpfFreq.store(freq); mParamsChanged.store(true); }
void AudioEngine::setLimiterThreshold(float threshold) { mLimiterThreshold.store(threshold); }
void AudioEngine::setNoiseGateThreshold(float threshold) { mNoiseGateThreshold.store(threshold); }
void AudioEngine::setMasterGain(float gain) { mMasterGain.store(gain); }
void AudioEngine::setEqualizerBandGain(int bandIndex, float gainDb) {
    if (bandIndex >= 0 && bandIndex < 5) {
        mManualBandGains[bandIndex].store(gainDb);
        mParamsChanged.store(true);
    }
}

void AudioEngine::setProfile(AudioProfile profile) {
    mAudioProfile.store(profile);
    for (int i = 0; i < 5; ++i) mProfileBandGains[i].store(0.0f);
    mVoiceBoostDb.store(0.0f);

    if (profile == AudioProfile::Voice) {
        mProfileBandGains[0].store(-12.0f);
        mProfileBandGains[1].store(-6.0f);
        mProfileBandGains[2].store(3.0f);
        mProfileBandGains[3].store(8.0f);
        mProfileBandGains[4].store(-3.0f);
        mVoiceBoostDb.store(10.0f);
    } else if (profile == AudioProfile::Music) {
        mProfileBandGains[0].store(4.0f);
        mProfileBandGains[1].store(0.0f);
        mProfileBandGains[2].store(-2.0f);
        mProfileBandGains[3].store(2.0f);
        mProfileBandGains[4].store(4.0f);
    } else if (profile == AudioProfile::TV) {
        mProfileBandGains[0].store(-6.0f);
        mProfileBandGains[1].store(0.0f);
        mProfileBandGains[2].store(4.0f);
        mProfileBandGains[3].store(6.0f);
        mProfileBandGains[4].store(-4.0f);
        mVoiceBoostDb.store(6.0f);
    }
    mParamsChanged.store(true);
}

void AudioEngine::setSensorFusion(bool enabled) {
    mSensorFusionEnabled.store(enabled);
}

void AudioEngine::setTargetLock(bool enabled) {
    mTargetLockEnabled.store(enabled);
    mParamsChanged.store(true);
}

float AudioEngine::getNextResampledRemoteSample() {
    if (mResamplePhase == 0) {
        mPrevRemoteSample = mCurrRemoteSample;
        if (mRemoteReadPos != mRemoteWritePos) {
            mCurrRemoteSample = mRemoteBuffer[mRemoteReadPos];
            mRemoteReadPos = (mRemoteReadPos + 1) % REMOTE_BUFFER_SIZE;
        } else {
            mCurrRemoteSample = 0.0f;
        }
    }
    float fraction = (float)(mResamplePhase) / 3.0f;
    float interpolated = mPrevRemoteSample + fraction * (mCurrRemoteSample - mPrevRemoteSample);
    mResamplePhase = (mResamplePhase + 1) % 3;
    return interpolated;
}

void AudioEngine::updateFilters() {
    if (mTargetLockEnabled.load()) {
        // Hyper-restrictive "Tunnel" filter for crowds
        mHighPass.setHighPass(300.0f, mSampleRate, 0.707f); // Cut all rumble/bass
        mLowPass.setLowPass(4000.0f, mSampleRate, 0.707f);  // Cut all hiss/air
        
        // Massive boost to core formants, kill everything else
        mEQBands[0].setPeaking(200.0f, mSampleRate, 1.0f, -24.0f);
        mEQBands[1].setPeaking(500.0f, mSampleRate, 1.0f, -12.0f);
        mEQBands[2].setPeaking(1500.0f, mSampleRate, 2.0f, 18.0f); // Tight Q, massive boost
        mEQBands[3].setPeaking(3000.0f, mSampleRate, 2.0f, 12.0f); // Tight Q, massive boost
        mEQBands[4].setPeaking(6000.0f, mSampleRate, 1.0f, -24.0f);
        
        mVoiceFilters[0].setPeaking(700.0f, mSampleRate, 0.4f, 5.0f);
        mVoiceFilters[1].setPeaking(3200.0f, mSampleRate, 0.4f, 5.0f);
    } else {
        // Normal profile + manual offset logic
        mHighPass.setHighPass(mHpfFreq.load(), mSampleRate, 0.707f);
        mLowPass.setLowPass(mLpfFreq.load(), mSampleRate, 0.707f);
        
        float freqs[5] = {200.0f, 500.0f, 1500.0f, 3000.0f, 6000.0f};
        for (int i = 0; i < 5; ++i) {
            float combinedGain = mManualBandGains[i].load() + mProfileBandGains[i].load();
            mEQBands[i].setPeaking(freqs[i], mSampleRate, 1.0f, combinedGain);
        }
        mVoiceFilters[0].setPeaking(700.0f, mSampleRate, 0.4f, mVoiceBoostDb.load() * 0.4f);
        mVoiceFilters[1].setPeaking(3200.0f, mSampleRate, 0.4f, mVoiceBoostDb.load() * 0.8f);
    }
    mParamsChanged.store(false);
}

inline float AudioEngine::processSample(float sample) {
    if (mCurrentRampGain < 1.0f) mCurrentRampGain += mRampStep;
    float out = sample * mPreAmpGain.load();

    // Noise Gate
    float absOut = std::abs(out);
    if (absOut > mNoiseGateThreshold.load()) {
        mGateHoldCounter = mGateHoldFrames;
    } else if (mGateHoldCounter > 0) {
        mGateHoldCounter--;
    }
    if (mGateHoldCounter <= 0) return 0.0f;

    // Filters
    out = mHighPass.process(out);
    out = mLowPass.process(out);
    out = mVoiceFilters[0].process(out);
    out = mVoiceFilters[1].process(out);
    for (int i = 0; i < 5; ++i) out = mEQBands[i].process(out);
    
    out *= mMasterGain.load() * mCurrentRampGain;

    // Clap Killer (Instant Limiter)
    float limit = mLimiterThreshold.load();
    if (out > limit) out = limit + (out - limit) * 0.1f;
    else if (out < -limit) out = -limit + (out + limit) * 0.1f;

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
    for (int i = 0; i < size; ++i) {
        float freq = 20.0f * powf(1000.0f, (float)i / (float)size);
        float eqMag = mHighPass.getMagnitude(freq, mSampleRate) * mLowPass.getMagnitude(freq, mSampleRate);
        for (int b = 0; b < 5; b++) eqMag *= mEQBands[b].getMagnitude(freq, mSampleRate);
        float vMag = mVoiceFilters[0].getMagnitude(freq, mSampleRate) * mVoiceFilters[1].getMagnitude(freq, mSampleRate);
        output[i] = eqMag * vMag;
    }
}

void AudioEngine::autoTune() {
    mManualBandGains[0].store(-18.0f);
    mManualBandGains[1].store(-8.0f);
    mManualBandGains[2].store(4.0f);
    mManualBandGains[3].store(12.0f);
    mManualBandGains[4].store(-6.0f);
    mVoiceBoostDb.store(15.0f);
    mParamsChanged.store(true);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    float *outputBuffer = static_cast<float *>(audioData);
    if (mParamsChanged.load()) updateFilters();

    float ambientRMS = 0.0f;
    bool fusion = mSensorFusionEnabled.load();
    InputSource source = mInputSource.load();

    if (source == InputSource::Watch || fusion) {
        float sumSq = 0;
        float remoteGain = mRemoteGain.load();
        for (int i = 0; i < numFrames; ++i) {
            float s = getNextResampledRemoteSample() * remoteGain;
            sumSq += s * s;
            if (source == InputSource::Watch) outputBuffer[i] = s;
        }
        ambientRMS = sqrtf(sumSq / (float)numFrames);
    }

    if (source != InputSource::Watch) {
        if (mRecordingStream) {
            auto result = mRecordingStream->read(outputBuffer, numFrames, 0);
            if (!result || result.value() < numFrames) {
                int32_t start = result ? result.value() : 0;
                for (int i = start; i < numFrames; i++) outputBuffer[i] = 0.0f;
            }
        } else {
            for (int i = 0; i < numFrames; i++) outputBuffer[i] = 0.0f;
        }
    }

    float baseThreshold = mNoiseGateThreshold.load();
    if (fusion) {
        float dynamicThresh = baseThreshold + (ambientRMS * 0.5f);
        mNoiseGateThreshold.store(dynamicThresh);
    }

    for (int i = 0; i < numFrames; ++i) {
        outputBuffer[i] = processSample(outputBuffer[i]);
    }

    if (fusion) mNoiseGateThreshold.store(baseThreshold);

    updateVisualization(outputBuffer, numFrames);
    return oboe::DataCallbackResult::Continue;
}
