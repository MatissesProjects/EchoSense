#ifndef ECHOSENSE_SPECTRALPROCESSOR_H
#define ECHOSENSE_SPECTRALPROCESSOR_H

#include <vector>
#include <complex>
#include <cmath>

class SpectralProcessor {
public:
    SpectralProcessor(int fftSize) : mSize(fftSize) {
        mBuffer.resize(mSize);
        mFftResult.resize(mSize);
        // Precompute twiddle factors for speed
        mTwiddles.resize(mSize / 2);
        for (int i = 0; i < mSize / 2; ++i) {
            float angle = -2.0f * (float)M_PI * i / mSize;
            mTwiddles[i] = std::complex<float>(cosf(angle), sinf(angle));
        }
    }

    void processBlock(float* data, float* noiseProfile, float reductionStrength) {
        // 1. Copy to complex buffer
        for (int i = 0; i < mSize; ++i) {
            mBuffer[i] = std::complex<float>(data[i], 0.0f);
        }

        // 2. Perform FFT
        fft(mBuffer, false);

        // 3. Spectral Subtraction / Gating
        for (int i = 0; i < mSize; ++i) {
            float magnitude = std::abs(mBuffer[i]);
            float phase = std::arg(mBuffer[i]);
            
            // Subtract noise floor or apply gate
            float threshold = noiseProfile[i] * reductionStrength;
            if (magnitude < threshold) {
                magnitude *= 0.1f; // Aggressive reduction
            } else {
                magnitude -= threshold * 0.5f; // Soft subtraction
            }
            
            mBuffer[i] = std::polar(std::max(0.0f, magnitude), phase);
        }

        // 4. Perform Inverse FFT
        fft(mBuffer, true);

        // 5. Copy back to real data
        for (int i = 0; i < mSize; ++i) {
            data[i] = mBuffer[i].real();
        }
    }

    void fft(std::vector<std::complex<float>>& x, bool invert) {
        int n = x.size();
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; j & bit; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) std::swap(x[i], x[j]);
        }

        for (int len = 2; len <= n; len <<= 1) {
            float ang = 2.0f * (float)M_PI / len * (invert ? -1 : 1);
            std::complex<float> wlen(cosf(ang), sinf(ang));
            for (int i = 0; i < n; i += len) {
                std::complex<float> w(1);
                for (int j = 0; j < len / 2; j++) {
                    std::complex<float> u = x[i + j], v = x[i + j + len / 2] * w;
                    x[i + j] = u + v;
                    x[i + j + len / 2] = u - v;
                    w *= wlen;
                }
            }
        }

        if (invert) {
            for (std::complex<float>& a : x) a /= (float)n;
        }
    }

private:
    int mSize;
    std::vector<std::complex<float>> mBuffer;
    std::vector<std::complex<float>> mFftResult;
    std::vector<std::complex<float>> mTwiddles;
};

#endif //ECHOSENSE_SPECTRALPROCESSOR_H
