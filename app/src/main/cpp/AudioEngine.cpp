#include "AudioEngine.h"
#include <android/log.h>

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(1);
    builder.setDataCallback(this);

    oboe::Result result = builder.openStream(&mStream);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to start stream. Error: %s", oboe::convertToText(result));
        return false;
    }

    return true;
}

void AudioEngine::stop() {
    if (mStream != nullptr) {
        mStream->close();
        mStream = nullptr;
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    // Process audio data here
    return oboe::DataCallbackResult::Continue;
}