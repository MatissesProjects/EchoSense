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
    float getVolumeLevel() const { return mCurrentVolume.load(); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    oboe::AudioStream *mRecordingStream = nullptr;
    oboe::AudioStream *mPlaybackStream = nullptr;
    
    // Parameters (atomic for thread safety)
    std::atomic<float> mNoiseGateThreshold{0.0f};
    std::atomic<float> mBandGains[5];
    std::atomic<float> mCurrentVolume{0.0f};

    // Internal processing
    float processSample(float sample);
    void calculateVolume(const float* data, int numFrames);
};

#endif //ECHOSENSE_AUDIOENGINE_H