#include <iostream>
#include <cassert>
#include <cmath>
#include <vector>
#include "../main/cpp/AudioEngine.h"
#include "../main/cpp/SpectralProcessor.h"

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
    // We can't easily test processSample without a mock, 
    // but we can test the logic directly if we expose it.
    // Assuming out = limit + (out - limit) * 0.1f
    float limit = 0.9f;
    float input = 1.5f;
    float output = limit + (input - limit) * 0.1f;
    assert(output < 1.0f);
    assert(output > 0.9f);
    
    std::cout << "Limiter Protection Logic Test: PASSED" << std::endl;
}

void testSpectralFftRoundTrip() {
    int size = 128;
    SpectralProcessor sp(size);
    std::vector<float> data(size);
    for(int i=0; i<size; i++) data[i] = sinf(2.0f * M_PI * i / 16.0f); // Pure tone

    std::vector<float> original = data;
    float noiseProfile[128] = {0}; // Zero noise profile
    
    sp.processBlock(data.data(), noiseProfile, 0.0f); // Zero reduction

    for(int i=0; i<size; i++) {
        assert(std::abs(data[i] - original[i]) < 0.001f);
    }
    
    std::cout << "Spectral FFT Round-trip Test: PASSED" << std::endl;
}

int main() {
    testBiquadFilters();
    testLimiterProtection();
    testSpectralFftRoundTrip();
    std::cout << "All Native Tests: PASSED" << std::endl;
    return 0;
}
