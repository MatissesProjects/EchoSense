#ifndef ECHOSENSE_SCENECLASSIFIER_H
#define ECHOSENSE_SCENECLASSIFIER_H

#include <vector>
#include <cmath>
#include <numeric>
#include <algorithm>

enum class SceneType {
    Quiet = 0,
    Voice = 1,
    Music = 2,
    Noisy = 3
};

class SceneClassifier {
public:
    SceneClassifier(int fftSize, float sampleRate) 
        : mFftSize(fftSize), mSampleRate(sampleRate) {
        mPrevMag.assign(fftSize, 0.0f);
    }

    SceneType classify(const float* magnitude, int size) {
        if (size < mFftSize) return SceneType::Quiet;

        float totalEnergy = 0.0f;
        float centroidNum = 0.0f;
        float spectralFlux = 0.0f;
        
        // Harmonicity check variables
        int peakCount = 0;
        float maxMag = 0.0f;

        // Log-spectral flatness variables
        float sumLogMag = 0.0f;
        float sumMag = 0.0f;

        for (int i = 1; i < mFftSize / 2; i++) {
            float mag = magnitude[i];
            totalEnergy += mag;
            centroidNum += mag * (float)i;
            spectralFlux += std::abs(mag - mPrevMag[i]);
            
            if (mag > 1e-6f) {
                sumLogMag += std::log(mag + 1e-9f);
                sumMag += mag;
            }

            if (mag > maxMag) maxMag = mag;
            
            // Simple peak detection for harmonicity (Voice/Music)
            if (i > 1 && i < mFftSize/2 - 1) {
                if (mag > magnitude[i-1] && mag > magnitude[i+1] && mag > 0.05f) {
                    peakCount++;
                }
            }
            
            mPrevMag[i] = mag;
        }

        if (totalEnergy < 0.01f) return SceneType::Quiet;

        float centroid = centroidNum / (totalEnergy + 1e-9f);
        float flatness = std::exp(sumLogMag / (mFftSize / 2.0f)) / (sumMag / (mFftSize / 2.0f) + 1e-9f);
        float normalizedFlux = spectralFlux / (totalEnergy + 1e-9f);

        // Classification Logic (Heuristic for the prototype)
        
        // 1. Music usually has high harmonicity (peaks) and lower flux than speech
        if (flatness < 0.1f && peakCount > 8 && normalizedFlux < 0.3f) {
            return SceneType::Music;
        }

        // 2. Voice has distinct spectral centroid (usually mid-range) and high flux (onsets/offsets)
        if (centroid > 5.0f && centroid < 25.0f && normalizedFlux > 0.4f) {
            return SceneType::Voice;
        }

        // 3. Noisy has high flatness (white noise) or very high centroid (hiss)
        if (flatness > 0.4f || centroid > 30.0f) {
            return SceneType::Noisy;
        }

        // Default to Voice if we hear something but aren't sure, as it's our primary use case
        return SceneType::Voice;
    }

    const char* getSceneName(SceneType scene) {
        switch(scene) {
            case SceneType::Quiet: return "Quiet";
            case SceneType::Voice: return "Voice Focus";
            case SceneType::Music: return "Music";
            case SceneType::Noisy: return "Crowded/Noisy";
            default: return "Unknown";
        }
    }

private:
    int mFftSize;
    float mSampleRate;
    std::vector<float> mPrevMag;
};

#endif //ECHOSENSE_SCENECLASSIFIER_H
