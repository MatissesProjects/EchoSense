# Track: Phase 7 - Advanced Adaptive & Neural Filtering

## Objective
Implement six major advanced filtering techniques to elevate EchoSense to medical-grade and high-end audio standards.

## Status
- [x] **Adaptive LMS Noise Cancellation**: Implemented in `LMSFilter.h`.
- [ ] **Spectral De-Reverberation**: Suppress late reflections.
- [ ] **Harmonic-Percussive Source Separation (HPSS)**: Separate voices from sharp noises.
- [ ] **Frequency Compression/Shifting**: Help with high-frequency hearing loss.
- [ ] **Multi-Band Neural Masking**: RNN-based gain masking.
- [ ] **Psychoacoustic Bass Enhancement**: Virtual pitch generation.

## Implementation Plan
1.  **LMS ANC** (Done): Completed using Watch reference mic.
2.  **De-Reverberation**: Implement a spectral magnitude decay estimator.
3.  **HPSS**: Implement median filtering on the spectrogram (horizontal for harmonics, vertical for percussives).
4.  **Freq Compression**: Use a non-linear warping function in the spectral domain.
5.  **Neural Masking**: Integrate a small pre-trained TFLite model or simple GRU logic.
6.  **Bass Boost**: Implement a non-linear device (NLD) to generate harmonics.
