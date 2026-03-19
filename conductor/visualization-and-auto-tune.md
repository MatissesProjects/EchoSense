# EchoSense: UI Visualization & Intelligent Control Plan

## Background & Motivation
To improve the user experience and effectiveness of the hearing assistant, we need better feedback and easier control. Visualizing the audio spectrum and the EQ curve helps users understand what they are filtering. A pause/restart button allows for quick comparisons, and an "Auto-Tune" feature provides intelligent filtering suggestions based on ambient noise analysis.

## Scope & Impact
This task adds real-time audio visualization and control refinements.
It includes:
- **UI Updates:**
    - A Start/Stop toggle button.
    - A custom `FrequencyVisualizerView` to show the audio spectrum and EQ curve.
    - An "Auto-Tune" button.
- **C++ DSP Updates:**
    - Implementation of an FFT (Fast Fourier Transform) to analyze incoming audio.
    - Calculation of the EQ filter chain's magnitude response.
    - Logic to analyze the spectrum and suggest optimal EQ gains.
- **JNI Updates:**
    - Methods to retrieve FFT and EQ curve data.
    - Method to trigger the "Auto-Tune" analysis.

## Implementation Steps

1. **C++ Spectrum Analysis (`AudioEngine.h/cpp`):**
   - Integrate a lightweight FFT implementation.
   - Buffer incoming audio for FFT processing.
   - Calculate the combined magnitude response of the 5-band Biquad chain.

2. **JNI Data Bridge (`native-lib.cpp`):**
   - Add `getFftData()` to return the latest spectrum magnitude.
   - Add `getEqCurveData()` to return the magnitude response of the EQ.

3. **Custom Visualizer View (`FrequencyVisualizerView.kt`):**
   - Create a custom Android View that draws:
     - The real-time spectrum (as a filled area or bars).
     - The EQ curve (as a colored line overlay).

4. **UI & Control Logic (`MainActivity.kt` & `activity_main.xml`):**
   - Add the Start/Stop and Auto-Tune buttons.
   - Implement polling in Kotlin to update the Visualizer View.
   - Implement `autoTune()` logic: Cut bands where persistent noise is detected and boost speech frequencies (1kHz-4kHz).

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify the visualizer shows real-time frequency activity.
- Verify the EQ curve shifts as you move the sliders.
- Test the "Auto-Tune" button in a noisy environment (e.g., near a fan) and verify it suggests/applies effective cuts.
