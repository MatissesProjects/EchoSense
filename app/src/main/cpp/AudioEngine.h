#ifndef ECHOSENSE_AUDIOENGINE_H
#define ECHOSENSE_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <cmath>
#include <vector>
#include <mutex>

#define VIS_BINS 64
#define REMOTE_BUFFER_SIZE 4096
#define NOISE_PROFILE_SIZE 128

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

// Biquad Filter Structure
struct Biquad {
    float b0, b1, b2, a1, a2;
    float x1, x2, y1, y2;

    Biquad() : b0(1), b1(0), b2(0), a1(0), a2(0), x1(0), x2(0), y1(0), y2(0) {}

    inline float process(float in) {
        float out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1; x1 = in; y2 = y1; y1 = out;
        return out;
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
    void setNoiseGateThreshold(float threshold);
    void setEqualizerBandGain(int bandIndex, float gainDb);
    void setMasterGain(float gain);
    void setProfile(AudioProfile profile);
    void setSensorFusion(bool enabled);
    float getVolumeLevel() const { return mCurrentVolume.load(); }

    void getFftData(float* output, int size);
    void getEqCurveData(float* output, int size);
    void autoTune();

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
    std::atomic<float> mNoiseGateThreshold{0.0f};
    std::atomic<float> mManualBandGains[5];
    std::atomic<float> mProfileBandGains[5];
    std::atomic<float> mMasterGain{1.0f};
    std::atomic<AudioProfile> mAudioProfile{AudioProfile::Custom};
    std::atomic<bool> mSensorFusionEnabled{false};
    std::atomic<float> mCurrentVolume{0.0f};
    std::atomic<bool> mParamsChanged{true};

    float mCurrentRampGain = 0.0f;
    const float mRampStep = 0.001f;
    int32_t mGateHoldCounter = 0;
    const int32_t mGateHoldFrames = 2400; // ~50ms at 48kHz

    Biquad mHighPass;
    Biquad mEQBands[5];
    Biquad mVoiceFilters[2];

    // Intelligent Noise Profile
    float mNoiseProfile[NOISE_PROFILE_SIZE] = {0};
    std::atomic<bool> mLearningNoise{false};

    float mVisData[VIS_BINS] = {0};
    std::mutex mVisMutex;

    void updateFilters();
    inline float processSample(float sample);
    void updateVisualization(const float* data, int numFrames);
};

#endif //ECHOSENSE_AUDIOENGINE_H