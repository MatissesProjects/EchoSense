# EchoSense: Foreground Continuity & Transcription Reliability Plan

## Background & Motivation
The user wants to ensure the hearing assistant remains active even if the screen turns off or if the app is in the background. Additionally, the transcription needs to be more reliable, and there's a request for a "Dim Mode" to keep the screen on without draining excessive battery.

## Scope & Impact
This task focuses on making EchoSense a background-capable service and improving the robustness of the AI transcription.
It includes:
- **Foreground Service:** Moving the `AudioEngine` lifecycle to a Foreground Service so it persists when the screen is off.
- **Transcription Watchdog:** Implementing a more aggressive restart logic for `SpeechRecognizer` to ensure it never stops listening.
- **Power Management:**
    - Using `FLAG_KEEP_SCREEN_ON` to prevent the screen from sleeping.
    - Implementing a "Super Dark" (Dim) mode by adjusting window brightness.
- **Foreground Notification:** Providing a persistent notification to allow the user to control the engine from the lock screen.

## Implementation Steps

1. **Create `EchoSenseService.kt`:**
   - Migrate `startAudioEngine` and `stopAudioEngine` calls here.
   - Implement `onStartCommand` to show a Foreground Notification.
   - Manage the `SpeechRecognizer` within the service (or coordinate with Activity).

2. **Transcription Reliability (`SpeechRecognizer`):**
   - In `onError`, check the error code. If it's a timeout or "no match," immediately call `startListening()` again.
   - Ensure the `RecognitionListener` is resilient to system-level interruptions.

3. **Screen Control (`MainActivity.kt`):**
   - Add a toggle for "Always On" mode.
   - When enabled, set `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)`.
   - Implement a "Dim Screen" button that sets `window.attributes.screenBrightness` to 0.01 (minimum visible).

4. **Manifest Updates:**
   - Register the service and add `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE` permissions (Android 14+ requirement).

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that audio processing continues even after pressing the power button.
- Verify that transcription resumes automatically after a period of silence.
- Verify that "Dim Mode" successfully darkens the screen while keeping the visualizer active.
