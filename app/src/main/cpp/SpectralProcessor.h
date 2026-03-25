#ifndef ECHOSENSE_SPECTRALPROCESSOR_H
#define ECHOSENSE_SPECTRALPROCESSOR_H

#include <vector>
#include <cmath>
#include <algorithm>

class SpectralProcessor {
public:
    SpectralProcessor(int fftSize) : mSize(fftSize) {
        mReal.resize(mSize);
        mImag.resize(mSize);
        mPrevMag.assign(mSize, 0.0f);
        mBitRev.resize(mSize);
        
        // Pre-allocate buffers to avoid real-time allocation
        mCurrentMag.resize(mSize);
        mCurrentPhase.resize(mSize);
        mNewMag.resize(mSize);
        mWarpedMag.resize(mSize);

        // Pseudo-Neural State
        mFastEnergy.assign(mSize, 0.0f);
        mSlowEnergy.assign(mSize, 0.0f);

        mHistorySize = 5; 
        mMagHistory.resize(mHistorySize, std::vector<float>(mSize, 0.0f));
        mHistoryIndex = 0;

        for (int i = 0; i < mSize; i++) {
            int j = 0;
            for (int k = 0; (1 << k) < mSize; k++) {
                if ((i >> k) & 1) j |= (mSize >> (k + 1));
            }
            mBitRev[i] = j;
        }

        mSinTable.resize(mSize);
        mCosTable.resize(mSize);
        for (int i = 0; i < mSize; i++) {
            float angle = 2.0f * (float)M_PI * i / mSize;
            mSinTable[i] = sinf(angle);
            mCosTable[i] = cosf(angle);
        }
    }

    // Fast approximation of expf for neural sigmoid
    inline float fastExp(float x) {
        union { uint32_t i; float f; } v;
        v.i = (uint32_t)(12102203 * x + 1064866805);
        return v.f;
    }

    void processBlock(float* __restrict data, float* __restrict noiseProfile, float reductionStrength, 
                     float spectralGateThresh, float dereverbStrength, float hpssStrength,
                     float freqWarpStrength, float neuralMaskStrength) {
        
        // 1. Load and FFT
        for (int i = 0; i < mSize; i++) {
            mReal[i] = data[i];
            mImag[i] = 0.0f;
        }

        runFft(mReal.data(), mImag.data(), false);

        float decay = 0.85f; 
        
        // 2. Magnitude and Phase calculation (No allocation)
        for (int i = 0; i < mSize; i++) {
            float r = mReal[i];
            float im = mImag[i];
            mCurrentMag[i] = sqrtf(r * r + im * im);
            // Use standard atan2f for now, but on pre-allocated buffer
            mCurrentPhase[i] = atan2f(im, r);
            mMagHistory[mHistoryIndex][i] = mCurrentMag[i];
            mNewMag[i] = mCurrentMag[i];
        }

        float alphaFast = 0.4f;
        float alphaSlow = 0.05f;

        // 3. Combined Filter Pass (Loop Fusion)
        for (int i = 0; i < mSize; i++) {
            float magnitude = mCurrentMag[i];
            if (magnitude < 1e-9f) {
                mPrevMag[i] *= decay;
                mFastEnergy[i] *= (1.0f - alphaFast);
                mSlowEnergy[i] *= (1.0f - alphaSlow);
                continue;
            }

            float tempMag = magnitude;

            // Neural Masking
            if (neuralMaskStrength > 0.01f) {
                mFastEnergy[i] = (1.0f - alphaFast) * mFastEnergy[i] + alphaFast * tempMag;
                mSlowEnergy[i] = (1.0f - alphaSlow) * mSlowEnergy[i] + alphaSlow * tempMag;
                float snr = mFastEnergy[i] / (mSlowEnergy[i] + 1e-6f);
                float mask = 1.0f / (1.0f + fastExp(-2.0f * (snr - 1.5f)));
                tempMag *= (1.0f - neuralMaskStrength) + (mask * neuralMaskStrength);
            }

            // HPSS (Median window)
            if (hpssStrength > 0.01f) {
                float h_window[5]; // history size is 5
                for(int t=0; t<5; t++) h_window[t] = mMagHistory[t][i];
                std::sort(h_window, h_window + 5);
                float harmonic = h_window[2];
                
                float v_window[3];
                v_window[0] = mMagHistory[mHistoryIndex][std::max(0, i-1)];
                v_window[1] = mMagHistory[mHistoryIndex][i];
                v_window[2] = mMagHistory[mHistoryIndex][std::min(mSize-1, i+1)];
                std::sort(v_window, v_window + 3);
                float percussive = v_window[1];

                float harmonicMask = (harmonic * harmonic) / (harmonic * harmonic + percussive * percussive + 1e-9f);
                tempMag *= (1.0f - hpssStrength) + (harmonicMask * hpssStrength);
            }

            // Dereverb
            if (dereverbStrength > 0.01f) {
                float lateReverb = mPrevMag[i] * decay;
                if (tempMag < lateReverb * dereverbStrength) tempMag *= 0.2f; 
                mPrevMag[i] = std::max(tempMag, lateReverb);
            }

            // Spectral Subtraction & Gate
            float noiseFloor = noiseProfile[i] * reductionStrength;
            if (tempMag < noiseFloor) tempMag *= 0.1f;
            else tempMag -= noiseFloor * 0.5f;

            if (spectralGateThresh > 0.001f && tempMag < spectralGateThresh) tempMag *= 0.05f;

            mNewMag[i] = std::max(0.0f, tempMag);
        }

        // 4. Frequency Warping
        if (freqWarpStrength > 0.01f) {
            for (int i = 0; i < mSize / 2; i++) {
                float normalizedFreq = (float)i / (mSize / 2.0f);
                float warpedFreq = powf(normalizedFreq, 1.0f + freqWarpStrength * 0.5f);
                int sourceBin = std::min(mSize / 2 - 1, (int)(warpedFreq * (mSize / 2.0f)));
                mWarpedMag[i] = mNewMag[i] * (1.0f - freqWarpStrength) + mNewMag[sourceBin] * freqWarpStrength;
            }
            // Reflect to upper half for real FFT symmetry (simplified)
            for (int i = 0; i < mSize / 2; i++) mNewMag[i] = mWarpedMag[i];
        }

        // 5. Reconstruct and IFFT
        for (int i = 0; i < mSize; i++) {
            mReal[i] = mNewMag[i] * cosf(mCurrentPhase[i]);
            mImag[i] = mNewMag[i] * sinf(mCurrentPhase[i]);
        }

        mHistoryIndex = (mHistoryIndex + 1) % mHistorySize;
        runFft(mReal.data(), mImag.data(), true);
        for (int i = 0; i < mSize; i++) data[i] = mReal[i];
    }

private:
    void runFft(float* __restrict real, float* __restrict imag, bool invert) {
        for (int i = 0; i < mSize; i++) {
            if (i < mBitRev[i]) {
                std::swap(real[i], real[mBitRev[i]]);
                std::swap(imag[i], imag[mBitRev[i]]);
            }
        }
        for (int len = 2; len <= mSize; len <<= 1) {
            int step = mSize / len;
            for (int i = 0; i < mSize; i += len) {
                for (int j = 0; j < len / 2; j++) {
                    int idx = j * step;
                    float w_r = mCosTable[idx];
                    float w_i = (invert ? -mSinTable[idx] : mSinTable[idx]);
                    float u_r = real[i + j];
                    float u_i = imag[i + j];
                    float v_r = real[i + j + len / 2] * w_r - imag[i + j + len / 2] * w_i;
                    float v_i = real[i + j + len / 2] * w_i + imag[i + j + len / 2] * w_r;
                    real[i + j] = u_r + v_r;
                    imag[i + j] = u_i + v_i;
                    real[i + j + len / 2] = u_r - v_r;
                    imag[i + j + len / 2] = u_i - v_i;
                }
            }
        }
        if (invert) {
            float invN = 1.0f / mSize;
            for (int i = 0; i < mSize; i++) {
                real[i] *= invN;
                imag[i] *= invN;
            }
        }
    }

    int mSize;
    std::vector<float> mReal;
    std::vector<float> mImag;
    std::vector<float> mPrevMag;
    std::vector<float> mFastEnergy;
    std::vector<float> mSlowEnergy;
    
    // Pre-allocated buffers for processBlock
    std::vector<float> mCurrentMag;
    std::vector<float> mCurrentPhase;
    std::vector<float> mNewMag;
    std::vector<float> mWarpedMag;

    int mHistorySize;
    std::vector<std::vector<float>> mMagHistory;
    int mHistoryIndex;
    std::vector<int> mBitRev;
    std::vector<float> mSinTable;
    std::vector<float> mCosTable;
};

#endif //ECHOSENSE_SPECTRALPROCESSOR_H
