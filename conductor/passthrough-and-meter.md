# EchoSense: Audio Passthrough & Input Meter Implementation Plan

## Background & Motivation
The current `AudioEngine` only implements an `Input` stream, so no audio is actually sent to the headphones. To verify audio capture, we need:
1.  **Full Duplex Passthrough:** An `Output` stream to send the processed input to the headphones.
2.  **Input Visualizer:** A real-time volume meter to confirm microphone activity.

## Scope & Impact
This task updates the core audio engine to support bidirectional audio and exposes metering data to the UI.
It includes:
- Updating `AudioEngine.h/cpp` to manage an `Output` stream.
- Implementing a simple `FullDuplexStream` logic in Oboe.
- Adding real-time RMS (volume) calculation.
- Exposing a `getVolumeLevel()` method via JNI.
- Adding a `ProgressBar` as a volume meter in `MainActivity.kt`.

## Implementation Steps

1. **Update C++ Engine (`AudioEngine.h/cpp`):**
   - Create a second `oboe::AudioStream` for `Output`.
   - Update `onAudioReady` to handle the data flow from `Input` to `Output`.
   - Implement `calculateRMS(float* data, int numFrames)` and store it in an atomic float.

2. **Add JNI Meter Bridge (`native-lib.cpp`):**
   - Add `getVolumeLevel()` native method.

3. **Update UI Layout (`activity_main.xml`):**
   - Add a horizontal `ProgressBar` (styled as a level meter) above the controls.

4. **Kotlin UI Polling (`MainActivity.kt`):**
   - Use a `Coroutine` or `Handler` to poll `getVolumeLevel()` every 30ms and update the progress bar.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify the volume meter "bounces" when you speak into the microphone.
- Connect wired USB-C headphones and verify you hear the audio passthrough.
- Verify the "Noise Gate" slider now correctly cuts the output sound.
