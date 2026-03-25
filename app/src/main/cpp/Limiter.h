#ifndef ECHOSENSE_LIMITER_H
#define ECHOSENSE_LIMITER_H

#include <vector>
#include <algorithm>
#include <cmath>

/**
 * Look-ahead Brick-Wall Limiter
 * 
 * Provides acoustic shock protection by clamping output amplitudes.
 * Uses a short look-ahead buffer to anticipate peaks and reduce gain smoothly.
 */
class Limiter {
public:
    Limiter(float sampleRate, float lookAheadMs = 1.0f, float releaseMs = 50.0f) 
        : mSampleRate(sampleRate) {
        mLookAheadSamples = std::max(1, (int)(lookAheadMs * sampleRate / 1000.0f));
        mDelayLine.assign(mLookAheadSamples, 0.0f);
        mReadPos = 0;
        mWritePos = 0;
        
        mReleaseCoeff = expf(-1.0f / (releaseMs * sampleRate / 1000.0f));
        mCurrentGain = 1.0f;
    }

    /**
     * Process a block of samples
     * @param data Buffer to process in-place
     * @param numFrames Number of samples in the buffer
     * @param threshold Maximum allowable amplitude (0.0 to 1.0)
     */
    void processBlock(float* data, int32_t numFrames, float threshold) {
        for (int i = 0; i < numFrames; ++i) {
            float input = data[i];
            
            // 1. Store input in delay line
            mDelayLine[mWritePos] = input;
            mWritePos = (mWritePos + 1) % mLookAheadSamples;
            
            // 2. Look ahead for the maximum peak in the buffer
            float peak = 0.0f;
            for (float s : mDelayLine) {
                float absS = fabsf(s);
                if (absS > peak) peak = absS;
            }
            
            // 3. Calculate target gain
            float targetGain = 1.0f;
            if (peak > threshold) {
                targetGain = threshold / peak;
            }
            
            // 4. Smooth gain (Instant attack due to look-ahead, exponential release)
            if (targetGain < mCurrentGain) {
                mCurrentGain = targetGain; // Instant attack
            } else {
                mCurrentGain = mCurrentGain * mReleaseCoeff + targetGain * (1.0f - mReleaseCoeff);
            }
            
            // 5. Apply gain to the DELAYED sample (the one we just read)
            float delayedSample = mDelayLine[mReadPos];
            data[i] = delayedSample * mCurrentGain;
            mReadPos = (mReadPos + 1) % mLookAheadSamples;
        }
    }

    void setRelease(float releaseMs) {
        mReleaseCoeff = expf(-1.0f / (releaseMs * mSampleRate / 1000.0f));
    }

private:
    float mSampleRate;
    int mLookAheadSamples;
    std::vector<float> mDelayLine;
    int mReadPos;
    int mWritePos;
    
    float mReleaseCoeff;
    float mCurrentGain;
};

#endif //ECHOSENSE_LIMITER_H
