# EchoSense: Advanced Voice Clarity & Adaptive Filtering Plan

## Background & Motivation
The user reports that the current audio capture isn't "strong" enough and wants more effective filtering and a dedicated "Voice Boost" before moving to Phase 2. We need to implement more aggressive speech-centric DSP and provide a simplified UI control for "Voice" versus "Ambient" balance.

## Scope & Impact
This task enhances the C++ DSP engine and simplifies the UI for better accessibility.
It includes:
- **C++ DSP Enhancements:**
    - Adding a **Pre-Amp Stage** to pull more signal from the mic before the noise gate.
    - Implementing a **Dedicated Speech Band-Pass Filter** (300Hz - 3.5kHz) that can be blended in.
    - Implementing a **Compressor/Limiter** to bring quiet speech up while keeping loud noises down.
- **UI Updates:**
    - Adding a dedicated "Voice Clarity Boost" slider.
    - Adding a "Sensitivity" slider (Pre-Amp).
- **JNI Updates:**
    - New methods to control the Pre-Amp and Voice Clarity blend.

## Implementation Steps

1. **Update C++ Engine (`AudioEngine.h/cpp`):**
   - Add `mPreAmpGain` (Sensitivity).
   - Add `mVoiceClarityBoost` (Blend/Gain for speech frequencies).
   - Add a dedicated `Biquad` for a wide speech-band boost.
   - Update `processSample` to:
     1. Apply Pre-Amp.
     2. Apply Noise Gate.
     3. Apply Fixed High-Pass.
     4. Apply 5-Band EQ.
     5. Apply Parallel Speech Boost (The "Voice Boost" slider).
     6. Apply final Master Gain & Hard Limiter.

2. **Update JNI Bridge (`native-lib.cpp`):**
   - Add `setPreAmpGain(float gain)` and `setVoiceBoost(float gainDb)`.

3. **Update UI Layout (`activity_main.xml`):**
   - Add "Mic Sensitivity" slider at the top.
   - Add a large "Voice Clarity Boost" slider above the EQ bands.

4. **Update Kotlin UI Logic (`MainActivity.kt`):**
   - Map new sliders to the C++ engine.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that "Mic Sensitivity" allows picking up faint sounds.
- Verify that "Voice Clarity Boost" makes speech significantly more prominent without boosting fan noise.
