# EchoSense: 5-Band Biquad EQ & Speech Optimization Plan

## Background & Motivation
The current "EQ" is just a volume multiplier. To effectively suppress background noise (like fans) and boost speech, we need real frequency-domain filtering. We will implement a cascade of 5 Biquad filters.

## Scope & Impact
This task replaces the POC volume multipliers with professional-grade DSP filters.
It includes:
- Implementing a `Biquad` filter class in C++.
- Setting up a 5-band filter chain:
    - **Band 1 (High-Pass):** Fixed at ~150Hz to cut fan rumble and low-end mud.
    - **Band 2 (Peaking):** Centered at 500Hz (Low-mids).
    - **Band 3 (Peaking):** Centered at 1.5kHz (Speech core).
    - **Band 4 (Peaking):** Centered at 3kHz (Clarity/Consonants).
    - **Band 5 (Low-Pass):** Fixed at ~8kHz to cut high-frequency hiss/static.
- Updating JNI to map UI slider values (-12dB to +12dB) to filter coefficients.

## Proposed Solution
We'll use the standard Biquad filter equations (Robert Bristow-Johnson's EQ Cookbook). The filters will be processed in series (cascaded).

## Implementation Steps

1. **Implement Biquad Class (`AudioEngine.h/cpp`):**
   - Define a `Biquad` struct with coefficients (a0, a1, a2, b0, b1, b2).
   - Implement `process(float sample)` method.
   - Implement `updateCoefficients(type, freq, Q, gainDb)` method.

2. **Integrate into Audio Pipeline (`AudioEngine.cpp`):**
   - Initialize 5 `Biquad` instances in `AudioEngine`.
   - In `onAudioReady`, pass each sample through all 5 filters.

3. **Map UI to DSP (`MainActivity.kt` & `native-lib.cpp`):**
   - Update UI labels to reflect frequency bands.
   - Scale sliders to represent Decibels (-12.0 to +12.0 dB).
   - Ensure the High-Pass (Band 1) and Low-Pass (Band 5) are active by default to clean the signal.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that the "High-Pass" (Band 1) significantly reduces fan rumble.
- Verify that boosting 3kHz (Band 4) makes speech more "crisp."
