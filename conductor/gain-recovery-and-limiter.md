# EchoSense: DSP Gain Recovery & Limiting Implementation Plan

## Background & Motivation
The switch to Biquad filters significantly improved noise suppression, but the output gain is now too low to be useful. We need to re-introduce a powerful Post-EQ Master Gain and a simple Peak Limiter to prevent digital distortion (clipping) when high gain is applied.

## Scope & Impact
This task restores the amplification capabilities while maintaining the new frequency filters.
It includes:
- Adding a `mMasterGain` multiplier in the C++ `AudioEngine`.
- Implementing a basic Hard Limiter in `processSample` to keep audio within the -1.0 to 1.0 float range.
- Re-scaling the UI to provide up to 20x (+26dB) of master amplification.
- Adding a "Master Boost" slider back to the top of the UI.

## Implementation Steps

1. **Update C++ Engine (`AudioEngine.h/cpp`):**
   - Add `std::atomic<float> mMasterGain{1.0f}`.
   - Update `processSample` to apply `mMasterGain` *after* the EQ filters.
   - Implement a simple `std::clamp(sample * mMasterGain, -1.0f, 1.0f)` to prevent clipping.

2. **Update JNI Bridge (`native-lib.cpp`):**
   - Add `setMasterGain(float gain)` native method.

3. **Update UI Layout (`activity_main.xml`):**
   - Add a "Master Gain" labeled SeekBar at the top, just below the volume meter.

4. **Update Kotlin UI Logic (`MainActivity.kt`):**
   - Map the new Master Gain slider to a 0.0x to 20.0x range.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that sliding "Master Gain" to the right restores the loud, clear audio output.
- Verify that the EQ bands still function correctly to shape the amplified sound.
