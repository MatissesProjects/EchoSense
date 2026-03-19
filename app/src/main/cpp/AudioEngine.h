#ifndef ECHOSENSE_AUDIOENGINE_H
#define ECHOSENSE_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();

    // Parameter updates
    void setNoiseGateThreshold(float threshold);
    void setEqualizerBandGain(int bandIndex, float gain);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    oboe::AudioStream *mStream = nullptr;
    
    // Parameters (atomic for thread safety between UI and Audio threads)
    std::atomic<float> mNoiseGateThreshold{0.0f};
    std::atomic<float> mBandGains[5];

    // Internal processing
    float processSample(float sample);
};

#endif //ECHOSENSE_AUDIOENGINE_H