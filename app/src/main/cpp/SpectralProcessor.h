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
        
        // Pseudo-Neural State (Fast/Slow Energy Estimators)
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

    void processBlock(float* data, float* noiseProfile, float reductionStrength, 
                     float spectralGateThresh, float dereverbStrength, float hpssStrength,
                     float freqWarpStrength, float neuralMaskStrength) {
        for (int i = 0; i < mSize; i++) {
            mReal[i] = data[i];
            mImag[i] = 0.0f;
        }

        runFft(mReal.data(), mImag.data(), false);

        float decay = 0.85f; 
        std::vector<float> currentMag(mSize);
        std::vector<float> currentPhase(mSize);

        for (int i = 0; i < mSize; i++) {
            currentMag[i] = sqrtf(mReal[i] * mReal[i] + mImag[i] * mImag[i]);
            currentPhase[i] = atan2f(mImag[i], mReal[i]);
            mMagHistory[mHistoryIndex][i] = currentMag[i];
        }

        std::vector<float> newMag = currentMag;

        // Neural-like Masking Params
        float alphaFast = 0.4f;
        float alphaSlow = 0.05f;

        for (int i = 0; i < mSize; i++) {
            float magnitude = currentMag[i];
            if (magnitude < 1e-9f) {
                mPrevMag[i] *= decay;
                mFastEnergy[i] *= (1.0f - alphaFast);
                mSlowEnergy[i] *= (1.0f - alphaSlow);
                continue;
            }

            // --- Multi-Band Neural Masking (Recursive Energy Ratio) ---
            if (neuralMaskStrength > 0.01f) {
                mFastEnergy[i] = (1.0f - alphaFast) * mFastEnergy[i] + alphaFast * magnitude;
                mSlowEnergy[i] = (1.0f - alphaSlow) * mSlowEnergy[i] + alphaSlow * magnitude;
                
                // Ratio of fast energy to slow energy (high ratio = likely speech transient)
                float snr_estimate = mFastEnergy[i] / (mSlowEnergy[i] + 1e-6f);
                // Sigmoid-like gain mask based on SNR estimate
                float mask = 1.0f / (1.0f + expf(-2.0f * (snr_estimate - 1.5f)));
                newMag[i] *= (1.0f - neuralMaskStrength) + (mask * neuralMaskStrength);
            }

            // --- HPSS ---
            if (hpssStrength > 0.01f) {
                std::vector<float> h_window(mHistorySize);
                for(int t=0; t<mHistorySize; t++) h_window[t] = mMagHistory[t][i];
                std::sort(h_window.begin(), h_window.end());
                float harmonic = h_window[mHistorySize/2];

                int v_size = 3;
                std::vector<float> v_window;
                for(int f=std::max(0, i-1); f<=std::min(mSize-1, i+1); f++) 
                    v_window.push_back(mMagHistory[mHistoryIndex][f]);
                std::sort(v_window.begin(), v_window.end());
                float percussive = v_window[v_window.size()/2];

                float harmonicMask = (harmonic * harmonic) / (harmonic * harmonic + percussive * percussive + 1e-9f);
                newMag[i] *= (1.0f - hpssStrength) + (harmonicMask * hpssStrength);
            }

            // --- Neural Gate ---
            if (spectralGateThresh > 0.001f && newMag[i] < spectralGateThresh) {
                newMag[i] *= 0.05f; 
            }

            // --- Spectral Subtraction ---
            float noiseFloor = noiseProfile[i] * reductionStrength;
            if (newMag[i] < noiseFloor) {
                newMag[i] *= 0.1f;
            } else {
                newMag[i] -= noiseFloor * 0.5f;
            }

            // --- Dereverb ---
            if (dereverbStrength > 0.01f) {
                float lateReverb = mPrevMag[i] * decay;
                if (newMag[i] < lateReverb * dereverbStrength) {
                    newMag[i] *= 0.2f; 
                }
                mPrevMag[i] = std::max(newMag[i], lateReverb);
            }
            newMag[i] = std::max(0.0f, newMag[i]);
        }

        // --- Frequency Compression (Warping) ---
        if (freqWarpStrength > 0.01f) {
            std::vector<float> warpedMag = newMag;
            for (int i = 0; i < mSize / 2; i++) {
                float normalizedFreq = (float)i / (mSize / 2.0f);
                float warpedFreq = powf(normalizedFreq, 1.0f + freqWarpStrength * 0.5f);
                int sourceBin = (int)(warpedFreq * (mSize / 2.0f));
                sourceBin = std::min(mSize / 2 - 1, sourceBin);
                warpedMag[i] = newMag[i] * (1.0f - freqWarpStrength) + newMag[sourceBin] * freqWarpStrength;
            }
            newMag = warpedMag;
        }

        for (int i = 0; i < mSize; i++) {
            mReal[i] = newMag[i] * cosf(currentPhase[i]);
            mImag[i] = newMag[i] * sinf(currentPhase[i]);
        }

        mHistoryIndex = (mHistoryIndex + 1) % mHistorySize;
        runFft(mReal.data(), mImag.data(), true);
        for (int i = 0; i < mSize; i++) data[i] = mReal[i];
    }

private:
    void runFft(float* real, float* imag, bool invert) {
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
    int mHistorySize;
    std::vector<std::vector<float>> mMagHistory;
    int mHistoryIndex;
    std::vector<int> mBitRev;
    std::vector<float> mSinTable;
    std::vector<float> mCosTable;
};

#endif //ECHOSENSE_SPECTRALPROCESSOR_H
