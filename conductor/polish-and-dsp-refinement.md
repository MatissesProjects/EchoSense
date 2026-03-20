# EchoSense: Hardware Link & DSP Refinement Plan

## Background & Motivation
Before proceeding to AI Summarization, we must resolve critical UX and hardware issues:
1.  **WearOS Deployment:** The watch app is built but not installed/visible.
2.  **Service Termination:** The app persists in the background indefinitely due to the Foreground Service.
3.  **DSP Scaling:** The EQ filters and Voice Boost are not dramatic enough or scaled poorly.

## Scope & Impact
This task focuses on "closing the loop" for the hardware array and professionalizing the DSP.
It includes:
- **WearOS:** Instructions/Scripts to install the `wear` module.
- **Lifecycle:** Adding an "Exit" button to stop the background service and close the app.
- **DSP:** 
    - Expanding EQ range to +/- 24dB.
    - Enhancing "Voice Boost" with a dual-stage peaking filter for "Body" (500Hz) and "Clarity" (3.5kHz).
    - Refining the "Auto-Tune" algorithm for more aggressive noise cancellation.

## Implementation Steps

1. **Service Exit Logic (`MainActivity.kt` & `activity_main.xml`):**
   - Add a "Close & Exit" button to the UI.
   - Implement `stopService(EchoSenseService)` and `finishAndRemoveTask()`.

2. **DSP Refinement (`AudioEngine.h/cpp`):**
   - Update `setPeaking` to allow higher gain.
   - Re-design the Voice Path: Use two peaking filters instead of a simple band-pass to "carve out" speech.
   - Implement a smoother "Auto-Tune" transition.

3. **UI Scaling (`MainActivity.kt`):**
   - Update slider mappings to support the new +/- 24dB range.
   - Add a "Presence" slider for fine-tuning the speech clarity.

4. **WearOS Deployment Check:**
   - Attempt to detect the watch via `adb` and provide installation commands.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Verify the "Exit" button removes the notification and closes the app.
- Verify that EQ sliders at max/min settings have a very dramatic effect on the audio.
- Verify the Voice Boost makes speech "pop" out of the mix.
