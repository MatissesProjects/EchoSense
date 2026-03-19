#ifndef ECHOSENSE_AUDIOENGINE_H
#define ECHOSENSE_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <cmath>

// Biquad Filter Structure (RBJ Cookbook)
struct Biquad {
    float b0, b1, b2, a1, a2;
    float x1, x2, y1, y2;

    Biquad() : b0(1), b1(0), b2(0), a1(0), a2(0), x1(0), x2(0), y1(0), y2(0) {}

    float process(float in) {
        float out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = in;
        y2 = y1;
        y1 = out;
        return out;
    }

    void setPeaking(float freq, float sampleRate, float Q, float gainDb) {
        float A = powf(10.0f, gainDb / 40.0f);
        float omega = 2.0f * M_PI * freq / sampleRate;
        float sn = sinf(omega);
        float cs = cosf(omega);
        float alpha = sn / (2.0f * Q);

        float b0_raw = 1.0f + alpha * A;
        float b1_raw = -2.0f * cs;
        float b2_raw = 1.0f - alpha * A;
        float a0_raw = 1.0f + alpha / A;
        float a1_raw = -2.0f * cs;
        float a2_raw = 1.0f - alpha / A;

        b0 = b0_raw / a0_raw;
        b1 = b1_raw / a0_raw;
        b2 = b2_raw / a0_raw;
        a1 = a1_raw / a0_raw;
        a2 = a2_raw / a0_raw;
    }

    void setHighPass(float freq, float sampleRate, float Q) {
        float omega = 2.0f * M_PI * freq / sampleRate;
        float sn = sinf(omega);
        float cs = cosf(omega);
        float alpha = sn / (2.0f * Q);

        float b0_raw = (1.0f + cs) / 2.0f;
        float b1_raw = -(1.0f + cs);
        float b2_raw = (1.0f + cs) / 2.0f;
        float a0_raw = 1.0f + alpha;
        float a1_raw = -2.0f * cs;
        float a2_raw = 1.0f - alpha;

        b0 = b0_raw / a0_raw;
        b1 = b1_raw / a0_raw;
        b2 = b2_raw / a0_raw;
        a1 = a1_raw / a0_raw;
        a2 = a2_raw / a0_raw;
    }
};

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();

    // Parameter updates
    void setNoiseGateThreshold(float threshold);
    void setEqualizerBandGain(int bandIndex, float gainDb);
    float getVolumeLevel() const { return mCurrentVolume.load(); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    oboe::AudioStream *mRecordingStream = nullptr;
    oboe::AudioStream *mPlaybackStream = nullptr;
    float mSampleRate = 48000.0f;

    // Parameters (atomic for thread safety)
    std::atomic<float> mNoiseGateThreshold{0.0f};
    std::atomic<float> mBandGains[5];
    std::atomic<float> mCurrentVolume{0.0f};
    std::atomic<bool> mParamsChanged{true};

    // DSP Components
    Biquad mHighPass;      // Fixed Low-Cut (Fan removal)
    Biquad mEQBands[5];    // 5-Band User EQ

    // Internal processing
    void updateFilters();
    float processSample(float sample);
    void calculateVolume(const float* data, int numFrames);
};

#endif //ECHOSENSE_AUDIOENGINE_H