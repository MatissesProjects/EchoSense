#ifndef ECHOSENSE_LMSFILTER_H
#define ECHOSENSE_LMSFILTER_H

#include <vector>
#include <algorithm>

/**
 * Adaptive LMS (Least Mean Squares) Filter
 * 
 * Used for Adaptive Noise Cancellation (ANC).
 * Primary Signal (d): Phone Mic (Signal + Noise)
 * Reference Signal (x): Watch Mic (Mostly Noise)
 * Output (e): d - y (Cleaned Signal)
 * where y is the filtered reference signal.
 */
class LMSFilter {
public:
    LMSFilter(int filterOrder = 64, float mu = 0.01f) 
        : mOrder(filterOrder), mMu(mu) {
        mWeights.assign(mOrder, 0.0f);
        mDelayLine.assign(mOrder, 0.0f);
    }

    /**
     * Process one sample of adaptive cancellation
     * @param primary The signal we want to clean (e.g. Phone Mic)
     * @param reference The noise reference (e.g. Watch Mic)
     * @return The error signal (cleaned primary)
     */
    inline float process(float primary, float reference) {
        // 1. Shift delay line
        for (int i = mOrder - 1; i > 0; --i) {
            mDelayLine[i] = mDelayLine[i - 1];
        }
        mDelayLine[0] = reference;

        // 2. Calculate filter output (y)
        float y = 0.0f;
        for (int i = 0; i < mOrder; ++i) {
            y += mWeights[i] * mDelayLine[i];
        }

        // 3. Calculate error (e) - this is our cleaned signal
        float e = primary - y;

        // 4. Update weights (W_next = W + mu * e * X)
        // Normalized LMS adaptation step
        float x_normSq = 0.0f;
        for (float x : mDelayLine) x_normSq += x * x;
        float adjustedMu = mMu / (x_normSq + 1e-6f);

        for (int i = 0; i < mOrder; ++i) {
            mWeights[i] += adjustedMu * e * mDelayLine[i];
        }

        return e;
    }

    void processBlock(float* primaryData, const float* referenceData, int32_t numFrames) {
        for (int i = 0; i < numFrames; ++i) {
            primaryData[i] = process(primaryData[i], referenceData[i]);
        }
    }

    void setLearningRate(float mu) { mMu = mu; }
    void reset() {
        std::fill(mWeights.begin(), mWeights.end(), 0.0f);
        std::fill(mDelayLine.begin(), mDelayLine.end(), 0.0f);
    }

private:
    int mOrder;
    float mMu; // Learning rate
    std::vector<float> mWeights;
    std::vector<float> mDelayLine;
};

#endif //ECHOSENSE_LMSFILTER_H
