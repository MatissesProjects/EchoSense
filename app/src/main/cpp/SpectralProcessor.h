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
        
        // Precompute bit-reversal table
        for (int i = 0; i < mSize; i++) {
            int j = 0;
            for (int k = 0; (1 << k) < mSize; k++) {
                if ((i >> k) & 1) j |= (mSize >> (k + 1));
            }
            mBitRev[i] = j;
        }

        // Precompute Sin/Cos tables
        mSinTable.resize(mSize);
        mCosTable.resize(mSize);
        for (int i = 0; i < mSize; i++) {
            float angle = 2.0f * (float)M_PI * i / mSize;
            mSinTable[i] = sinf(angle);
            mCosTable[i] = cosf(angle);
        }
    }

    void processBlock(float* data, float* noiseProfile, float reductionStrength, float spectralGateThresh, float dereverbStrength) {
        // 1. Prepare Buffer
        for (int i = 0; i < mSize; i++) {
            mReal[i] = data[i];
            mImag[i] = 0.0f;
        }

        // 2. FFT
        runFft(mReal.data(), mImag.data(), false);

        // 3. Optimized Spectral Processing
        float decay = 0.85f; // T60 decay approximation
        for (int i = 0; i < mSize; i++) {
            float r = mReal[i];
            float im = mImag[i];
            float magnitude = sqrtf(r * r + im * im);
            
            if (magnitude < 1e-9f) {
                mPrevMag[i] *= decay;
                continue;
            }

            float newMag = magnitude;
            
            // --- Neural Multi-band Gate ---
            if (spectralGateThresh > 0.001f && magnitude < spectralGateThresh) {
                newMag *= 0.05f; 
            }

            // --- Spectral Subtraction (Noise) ---
            float noiseFloor = noiseProfile[i] * reductionStrength;
            if (newMag < noiseFloor) {
                newMag *= 0.1f;
            } else {
                newMag -= noiseFloor * 0.5f;
            }

            // --- Spectral De-Reverberation ---
            if (dereverbStrength > 0.01f) {
                float lateReverb = mPrevMag[i] * decay;
                if (newMag < lateReverb * dereverbStrength) {
                    newMag *= 0.2f; // Suppress echo
                }
                mPrevMag[i] = std::max(newMag, lateReverb);
            }
            
            newMag = std::max(0.0f, newMag);
            float scale = newMag / magnitude;
            mReal[i] *= scale;
            mImag[i] *= scale;
        }

        // 4. Inverse FFT
        runFft(mReal.data(), mImag.data(), true);

        // 5. Copy back
        for (int i = 0; i < mSize; i++) {
            data[i] = mReal[i];
        }
    }

private:
    void runFft(float* real, float* imag, bool invert) {
        // Bit-reversal permutation
        for (int i = 0; i < mSize; i++) {
            if (i < mBitRev[i]) {
                std::swap(real[i], real[mBitRev[i]]);
                std::swap(imag[i], imag[mBitRev[i]]);
            }
        }

        // Cooley-Tukey Iterative FFT
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
    std::vector<int> mBitRev;
    std::vector<float> mSinTable;
    std::vector<float> mCosTable;
};

#endif //ECHOSENSE_SPECTRALPROCESSOR_H
