# EchoSense: WebRTC C++ VAD Integration Plan

## Background & Motivation
For high-performance audio apps like EchoSense, **Voice Activity Detection (VAD)** is critical. Using a specialized C++ VAD (like the WebRTC VAD) allows the system to:
1.  **Save CPU/Battery:** Only perform complex AI (Spectral Processing, Transcription) when human speech is actually present.
2.  **Cleaner Transcription:** Prevent background noise and silence from being processed by the transcription engine.
3.  **UI Feedback:** Show the user exactly when their voice (or the interlocutor's) is being detected.

## Scope & Impact
This task focuses on the C++ engine in the `app` module.
It includes:
- Integrating a lightweight C++ VAD implementation into the `echosense_native` library.
- Updating `AudioEngine.cpp` to use the VAD status to gate downstream processing.
- Adding a `isSpeechDetected` callback from JNI to the Kotlin UI for visual indication.

## Implementation Steps

1. **VAD Implementation (`app/src/main/cpp/VAD.h`):**
   - Create a simplified C++ wrapper for WebRTC's VAD logic (or a high-quality energy-based VAD with pitch detection).
   - Support different "aggressiveness" levels (0-3).

2. **Engine Integration (`AudioEngine.cpp`):**
   - Call `mVad->process()` on each 10-30ms frame in the `onAudioReady` callback.
   - Use the VAD result to:
     - Pause/Resume the `SpeechRecognizer` (via JNI).
     - Inform the `SceneClassifier` of speech presence.

3. **JNI / Kotlin Bridge:**
   - Add a JNI method `isSpeechActive()` for the UI to query or use a listener pattern.
   - Show a "Voice Detected" ring around the visualizer in the phone app.

## Verification
- Run native unit tests for the VAD logic (`limiter_test` equivalent).
- Verify on a real device that silence results in 0% CPU for heavy AI tasks.
- Verify that transcription still works perfectly when speech begins.
