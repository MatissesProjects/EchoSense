#ifndef ECHOSENSE_AUDIOENGINE_H
#define ECHOSENSE_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <cmath>
#include <vector>
#include <mutex>
#include "SpectralProcessor.h"

#define VIS_BINS 64
#define REMOTE_BUFFER_SIZE 4096
#define FFT_SIZE 128

enum class InputSource {
    Default = 0,
    Phone = 1,
    Watch = 2
};

enum class AudioProfile {
    Voice = 0,
    Music = 1,
    TV = 2,
    Custom = 3
};

// Biquad Filter Structure - Optimized Direct Form II Transposed
struct Biquad {
    float b0, b1, b2, a1, a2;
    float z1, z2; // State variables

    Biquad() : b0(1), b1(0), b2(0), a1(0), a2(0), z1(0), z2(0) {}

    inline float process(float in) {
        float out = in * b0 + z1;
        z1 = in * b1 - out * a1 + z2;
        z2 = in * b2 - out * a2;
        return out;
    }
    
    // Process a block of samples (allows compiler auto-vectorization)
    void processBlock(float* data, int32_t numFrames) {
        for (int i = 0; i < numFrames; i++) {
            float in = data[i];
            float out = in * b0 + z1;
            z1 = in * b1 - out * a1 + z2;
            z2 = in * b2 - out * a2;
            data[i] = out;
        }
    }

    void setPeaking(float freq, float sampleRate, float Q, float gainDb) {
        float A = powf(10.0f, gainDb / 40.0f);
        float omega = 2.0f * (float)M_PI * freq / sampleRate;
        float sn = sinf(omega);
        float cs = cosf(omega);
        float alpha = sn / (2.0f * Q);
        float b0_raw = 1.0f + alpha * A;
        float b1_raw = -2.0f * cs;
        float b2_raw = 1.0f - alpha * A;
        float a0_raw = 1.0f + alpha / A;
        float a1_raw = -2.0f * cs;
        float a2_raw = 1.0f - alpha / A;
        b0 = b0_raw / a0_raw; b1 = b1_raw / a0_raw; b2 = b2_raw / a0_raw;
        a1 = a1_raw / a0_raw; a2 = a2_raw / a0_raw;
    }

    void setHighPass(float freq, float sampleRate, float Q) {
        float omega = 2.0f * (float)M_PI * freq / sampleRate;
        float sn = sinf(omega);
        float cs = cosf(omega);
        float alpha = sn / (2.0f * Q);
        float b0_raw = (1.0f + cs) / 2.0f;
        float b1_raw = -(1.0f + cs);
        float b2_raw = (1.0f + cs) / 2.0f;
        float a0_raw = 1.0f + alpha;
        float a1_raw = -2.0f * cs;
        float a2_raw = 1.0f - alpha;
        b0 = b0_raw / a0_raw; b1 = b1_raw / a0_raw; b2 = b2_raw / a0_raw;
        a1 = a1_raw / a0_raw; a2 = a2_raw / a0_raw;
    }

    void setLowPass(float freq, float sampleRate, float Q) {
        float omega = 2.0f * (float)M_PI * freq / sampleRate;
        float sn = sinf(omega);
        float cs = cosf(omega);
        float alpha = sn / (2.0f * Q);
        float b0_raw = (1.0f - cs) / 2.0f;
        float b1_raw = 1.0f - cs;
        float b2_raw = (1.0f - cs) / 2.0f;
        float a0_raw = 1.0f + alpha;
        float a1_raw = -2.0f * cs;
        float a2_raw = 1.0f - alpha;
        b0 = b0_raw / a0_raw; b1 = b1_raw / a0_raw; b2 = b2_raw / a0_raw;
        a1 = a1_raw / a0_raw; a2 = a2_raw / a0_raw;
    }

    float getMagnitude(float freq, float sampleRate) {
        float omega = 2.0f * (float)M_PI * freq / sampleRate;
        float cos_w = cosf(omega);
        float cos_2w = cosf(2.0f * omega);
        float num = b0*b0 + b1*b1 + b2*b2 + 2.0f*(b0*b1 + b1*b2)*cos_w + 2.0f*b0*b2*cos_2w;
        float den = 1.0f + a1*a1 + a2*a2 + 2.0f*(a1 + a1*a2)*cos_w + 2.0f*a2*cos_2w;
        return sqrtf(fmaxf(0.0f, num / den));
    }
};

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();
    bool isRunning() const { return mIsRunning.load(); }

    void setInputSource(InputSource source);
    void setInputDevice(int32_t deviceId);
    void setRemoteGain(float gain);
    void writeRemoteAudio(const float* data, int32_t numFrames);

    void setPreAmpGain(float gain);
    void setVoiceBoost(float gainDb);
    void setHpfFreq(float freq);
    void setLpfFreq(float freq);
    void setLimiterThreshold(float threshold);
    void setNoiseGateThreshold(float threshold);
    void setSpectralReduction(float strength);
    void setSpectralGateThreshold(float threshold);
    void setEqualizerBandGain(int bandIndex, float gainDb);
    void setMasterGain(float gain);
    void setProfile(AudioProfile profile);
    void setSensorFusion(bool enabled);
    void setTargetLock(bool enabled);
    void setTargetSpeaker(int speakerId);
    void setFocusLevel(float level);
    void setMbCompression(float ratio);
    void setBeamforming(bool enabled);
    void setTransientSuppression(float strength);
    void learnNoise();
    float getVolumeLevel() const { return mCurrentVolume.load(); }
    struct SpeakerInfo {
        int id;
        float energyPhone;
        float energyWatch;
        bool isActive;
    };

    void getSpeakerInfo(SpeakerInfo* outSpeakers, int maxSpeakers);
    int getDominantMic() const; // Returns 0 for Phone, 1 for Watch

    void getFftData(float* output, int size);
    void getEqCurveData(float* output, int size);
    void autoTune();

    float getIsolationGainDb() const { return mIsolationGainDb.load(); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    oboe::AudioStream *mRecordingStream = nullptr;
    oboe::AudioStream *mPlaybackStream = nullptr;
    std::atomic<bool> mIsRunning{false};
    float mSampleRate = 48000.0f;
    
    std::atomic<InputSource> mInputSource{InputSource::Default};
    std::atomic<int32_t> mInputDeviceId{oboe::kUnspecified};
    std::atomic<float> mRemoteGain{2.0f};

    float mRemoteBuffer[REMOTE_BUFFER_SIZE] = {0};
    std::atomic<int32_t> mRemoteReadPos{0};
    std::atomic<int32_t> mRemoteWritePos{0};
    
    // Resampling state (16kHz -> 48kHz)
    float mPrevRemoteSample = 0.0f;
    float mCurrRemoteSample = 0.0f;
    int mResamplePhase = 0;

    float getNextResampledRemoteSample();

    std::atomic<float> mPreAmpGain{1.0f};
    std::atomic<float> mVoiceBoostDb{0.0f};
    std::atomic<float> mHpfFreq{150.0f};
    std::atomic<float> mLpfFreq{12000.0f};
    std::atomic<float> mLimiterThreshold{0.9f};
    std::atomic<float> mNoiseGateThreshold{0.0f};
    std::atomic<float> mSpectralReductionStrength{0.0f};
    std::atomic<float> mSpectralGateThreshold{0.0f};
    std::atomic<float> mManualBandGains[5];
    std::atomic<float> mProfileBandGains[5];
    std::atomic<float> mMasterGain{1.0f};
    std::atomic<AudioProfile> mAudioProfile{AudioProfile::Custom};
    std::atomic<bool> mSensorFusionEnabled{false};
    std::atomic<bool> mTargetLockEnabled{false};
    std::atomic<int> mTargetSpeakerId{-1}; // -1 for none, 0 for Speaker A, 1 for Speaker B
    std::atomic<bool> mBeamformingEnabled{false};
    std::atomic<float> mMbCompressionRatio{1.0f};
    std::atomic<float> mFocusLevel{0.0f};
    std::atomic<float> mCurrentVolume{0.0f};
    std::atomic<float> mPhoneEnergy{0.0f};
    std::atomic<float> mWatchEnergy{0.0f};
    std::atomic<float> mIsolationGainDb{0.0f};
    std::atomic<bool> mParamsChanged{true};

    std::atomic<float> mTransientSuppressionStrength{0.0f};
    float mEnergyEnvelope = 0.0f;
    float mSuppressionGain = 1.0f;

    float mCurrentRampGain = 0.0f;
    const float mRampStep = 0.001f;
    int32_t mGateHoldCounter = 0;
    const int32_t mGateHoldFrames = 2400; // ~50ms at 48kHz

    Biquad mHighPass;
    Biquad mLowPass;
    Biquad mEQBands[5];
    Biquad mVoiceFilters[2];

    // Spectral AI
    SpectralProcessor* mSpectralProcessor = nullptr;
    float mNoiseProfile[FFT_SIZE] = {0};
    std::atomic<bool> mLearningNoise{false};
    int mLearningCounter = 0;

    float mVisData[VIS_BINS] = {0};
    std::mutex mVisMutex;

    void updateFilters();
    inline float processSample(float sample);
    void updateVisualization(const float* data, int numFrames);
};

#endif //ECHOSENSE_AUDIOENGINE_H
