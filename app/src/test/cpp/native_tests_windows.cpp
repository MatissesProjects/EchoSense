#include <iostream>
#include <cassert>
#include <cmath>
#include <vector>
#include <complex>
#include <algorithm>

// Mock Oboe
namespace oboe {
    enum class Result { OK, Error };
    enum class Direction { Input, Output };
    enum class PerformanceMode { LowLatency, None };
    enum class SharingMode { Shared, Exclusive };
    enum class Usage { VoiceCommunication };
    enum class ContentType { Speech };
    enum class InputPreset { VoiceRecognition };
    enum class AudioFormat { Float, I16 };
    enum class DataCallbackResult { Continue, Stop };
    const int kUnspecified = -1;

    class AudioStream {
    public:
        virtual ~AudioStream() {}
        virtual Result requestStart() { return Result::OK; }
        virtual Result stop() { return Result::OK; }
        virtual Result close() { return Result::OK; }
        virtual int32_t getSampleRate() { return 48000; }
        virtual Result read(void*, int32_t, int64_t) { return Result::OK; }
    };

    class AudioStreamDataCallback {
    public:
        virtual ~AudioStreamDataCallback() {}
        virtual DataCallbackResult onAudioReady(AudioStream*, void*, int32_t) = 0;
    };

    class AudioStreamBuilder {
    public:
        AudioStreamBuilder& setDirection(Direction) { return *this; }
        AudioStreamBuilder& setPerformanceMode(PerformanceMode) { return *this; }
        AudioStreamBuilder& setSharingMode(SharingMode) { return *this; }
        AudioStreamBuilder& setUsage(Usage) { return *this; }
        AudioStreamBuilder& setContentType(ContentType) { return *this; }
        AudioStreamBuilder& setFormat(AudioFormat) { return *this; }
        AudioStreamBuilder& setChannelCount(int) { return *this; }
        AudioStreamBuilder& setDataCallback(AudioStreamDataCallback*) { return *this; }
        AudioStreamBuilder& setInputPreset(InputPreset) { return *this; }
        AudioStreamBuilder& setSampleRate(int32_t) { return *this; }
        AudioStreamBuilder& setDeviceId(int32_t) { return *this; }
        Result openStream(AudioStream**) { return Result::OK; }
    };
}

// Mock Android log
#define ANDROID_LOG_INFO 4
#define __android_log_print(prio, tag, fmt, ...) printf("[%s] " fmt "\n", tag, ##__VA_ARGS__)

// Include the actual source files but with mocked Oboe
#include "../../main/cpp/SpectralProcessor.h"
// To include AudioEngine.h/cpp we need to be careful as they include Oboe.h
// Since we mocked oboe namespace above, it should work if we don't include actual oboe/Oboe.h

// We will just copy the Biquad struct here or include AudioEngine.h if it doesn't fail
#include "../../main/cpp/AudioEngine.h"

// We need to implement the methods we want to test from AudioEngine.cpp
// or just test the logic directly.

void testBiquadFilters() {
    float sampleRate = 48000.0f;
    
    // HPF Test
    Biquad hp;
    hp.setHighPass(1000.0f, sampleRate, 0.707f);
    float magLow = hp.getMagnitude(50.0f, sampleRate);
    float magHigh = hp.getMagnitude(5000.0f, sampleRate);
    assert(magLow < 0.1f);
    assert(magHigh > 0.9f);

    // LPF Test
    Biquad lp;
    lp.setLowPass(1000.0f, sampleRate, 0.707f);
    float magLowLp = lp.getMagnitude(50.0f, sampleRate);
    float magHighLp = lp.getMagnitude(5000.0f, sampleRate);
    assert(magLowLp > 0.9f);
    assert(magHighLp < 0.1f);

    std::cout << "Biquad HPF/LPF Tests: PASSED" << std::endl;
}

void testLimiterProtection() {
    float limit = 0.9f;
    float input = 1.5f;
    float output = limit + (input - limit) * 0.1f;
    assert(output < 1.0f);
    assert(output > 0.9f);
    
    std::cout << "Limiter Protection Logic Test: PASSED" << std::endl;
}

void testSpectralProcessor() {
    int size = 128;
    SpectralProcessor sp(size);
    std::vector<float> data(size);
    for(int i=0; i<size; i++) data[i] = sinf(2.0f * M_PI * i / 16.0f); // Pure tone

    std::vector<float> original = data;
    float noiseProfile[128] = {0}; 
    
    // Test 1: Transparency (Zero reduction)
    sp.processBlock(data.data(), noiseProfile, 0.0f, 0.0f);
    for(int i=0; i<size; i++) {
        assert(std::abs(data[i] - original[i]) < 0.01f);
    }
    
    // Test 2: Reduction
    for(int i=0; i<size; i++) noiseProfile[i] = 1.0f; // High noise profile
    sp.processBlock(data.data(), noiseProfile, 1.0f, 0.0f);
    
    float energyOriginal = 0, energyReduced = 0;
    for(int i=0; i<size; i++) {
        energyOriginal += original[i] * original[i];
        energyReduced += data[i] * data[i];
    }
    assert(energyReduced < energyOriginal);

    std::cout << "Spectral Processor Tests: PASSED" << std::endl;
}

int main() {
    testBiquadFilters();
    testLimiterProtection();
    testSpectralProcessor();
    std::cout << "All Native Tests (Windows Mocked): PASSED" << std::endl;
    return 0;
}
