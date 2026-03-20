# EchoSense: Hardware Resource Sharing & Lifecycle Stability Plan

## Background & Motivation
The user reports three critical stability issues:
1.  **Mic Resource Conflict:** Live transcription only works when the DSP engine is OFF. This is because both are trying to claim exclusive access to the microphone.
2.  **Restart Logic:** The engine doesn't "kick in" after a toggle until a slider is moved.
3.  **Gain Spikes:** Returning to the app after a screen-off event causes a massive gain burst.

## Scope & Impact
This task focuses on resource sharing and robust lifecycle management.
It includes:
- Switching Oboe to **Shared Mode** to allow the Android `SpeechRecognizer` to access the mic simultaneously.
- Implementing robust **Activity Lifecycle** handling (`onPause`/`onResume`) to safely release and reclaim hardware.
- Ensuring all DSP parameters are **force-applied** upon every engine start.
- Adding a "fade-in" or "soft-start" to the gain stage to prevent auditory shocks.

## Implementation Steps

1. **Enable Audio Sharing (`AudioEngine.cpp`):**
   - Change `SharingMode::Exclusive` to `SharingMode::Shared`.
   - While `Shared` has slightly higher latency (~10-20ms more), it is required for simultaneous microphone access by multiple system components.

2. **Fix Restart & Parameter State (`AudioEngine.h/cpp`):**
   - Ensure `mParamsChanged` is always true on `start()`.
   - Add a `forceUpdateParameters()` method called immediately after stream start.

3. **Handle Screen-Off & Gain Spikes (`MainActivity.kt`):**
   - Implement `onPause()` to stop the engine and `onResume()` to restart it.
   - Initialize sliders with safe defaults.
   - Implement a "Soft Start" in C++: Initialize gain at 0.0 and ramp to target over 100ms.

4. **Transcription Sync:**
   - Keep `SpeechRecognizer` active. In `Shared` mode, Android should now allow both to receive data.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that transcription works while the engine is running.
- Verify the engine starts immediately without moving a slider.
- Verify that turning the screen off and on does not cause a volume explosion.
