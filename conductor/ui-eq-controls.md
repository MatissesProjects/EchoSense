# EchoSense: 5-Band EQ & Noise Gate UI Implementation Plan

## Background & Motivation
Phase 1 of EchoSense requires a user interface to control the low-latency audio processing. We need visual controls for a 5-band EQ and a Noise Gate. These controls will later send parameters to the C++ DSP engine via JNI.

## Scope & Impact
This task covers the Android UI implementation in Kotlin.
It includes:
- Updating `activity_main.xml` with sliders (SeekBars) for:
    - 5 EQ bands (e.g., 100Hz, 500Hz, 1kHz, 3kHz, 8kHz).
    - Noise Gate threshold.
- Updating `MainActivity.kt` to:
    - Listen to slider changes.
    - Forward values to the native C++ engine.
- Updating `native-lib.cpp` and `AudioEngine.h/cpp` to:
    - Receive and store these parameters.
    - Prepare for the DSP filtering logic.

## Proposed Solution
We'll use standard Android `SeekBar` components for a clean, slider-based control panel. The values will be mapped to normalized ranges (e.g., 0.0 to 1.0) before being passed to C++.

## Implementation Steps

1. **Update UI Layout (`activity_main.xml`):**
   - Add a `ScrollView` containing a `LinearLayout`.
   - Add labeled `SeekBar` components for:
     - Noise Gate (Threshold).
     - Band 1 (Low Shelf/Bass).
     - Band 2 (Mid-Low).
     - Band 3 (Mid).
     - Band 4 (Mid-High).
     - Band 5 (High Shelf/Treble).

2. **JNI Parameter Bridge (`native-lib.cpp`):**
   - Add native methods:
     - `setNoiseGateThreshold(float threshold)`
     - `setEqualizerBandGain(int bandIndex, float gain)`

3. **C++ Engine Storage (`AudioEngine.h/cpp`):**
   - Add private member variables to store the current threshold and band gains.
   - Implement setter methods in `AudioEngine`.

4. **Kotlin UI Logic (`MainActivity.kt`):**
   - Bind the `SeekBar` listeners in `onCreate`.
   - Call the native methods whenever a slider is moved.

## Verification
- Run `.\gradlew.bat assembleDebug` to ensure both Kotlin and C++ changes compile.
- Deploy to the phone and verify the UI appears with all 6 sliders.
- (Optional) Use logcat to verify that JNI calls are reaching the C++ engine correctly.