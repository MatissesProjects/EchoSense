# EchoSense: Intelligent Scene Awareness & Battery Optimization Plan

## Background & Motivation
The original `PLAN.md` (Phase 5 and 8) describes "Burst" mode for WearOS to save battery and "Comb Filtering Mitigation" for Bluetooth. Additionally, while the C++ engine has a `SceneClassifier`, its integration with the UI and DSP transitions can be more seamless and user-visible.

## Scope & Impact
This task focuses on the "polish" phase of the adaptive system.
It includes:
- Implementing a "Burst" recording trigger on the watch (via button or wrist raise).
- Fine-tuning the C++ `SceneClassifier` thresholds for "Quiet", "Voice", "Music", and "Noisy" environments.
- Updating the phone UI to better visualize the detected scene and its impact on current settings.
- Implementing "Comb Filtering Mitigation" logic in the C++ engine for Bluetooth.

## Implementation Steps

1. **Watch "Burst" Mode Implementation:**
   - Add a "Burst Mode" toggle on the watch.
   - When active, audio is only streamed to the phone for 30-60 seconds after a trigger (manual tap or wrist raise).
   - Use `Sensor.TYPE_SIGNIFICANT_MOTION` or simple accelerometer threshold for auto-trigger.

2. **C++ Scene Classifier & Transitions (`AudioEngine.cpp`):**
   - Refine the `SceneClassifier` thresholds in C++ based on the Pixel 9's mic characteristics.
   - Smooth out transitions between profiles (e.g., crossfading between "Quiet" and "Voice Focus") to prevent jarring audio jumps.

3. **Comb Filtering Mitigation (Bluetooth):**
   - Add a specialized delay buffer in C++ that aligns the processed audio with the passive leakage sound when using Bluetooth LE Audio.
   - Add a manual "Phase Alignment" slider to the phone's "Wireless Optimization" section for user fine-tuning.

4. **Phone UI Enhancements (`MainActivity.kt`):**
   - Add a prominent "Current AI Context" card to the main screen.
   - Show a "Health Check" status for Bluetooth latency and Watch connectivity.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy to both devices.
- Verify watch battery consumption drops significantly when "Burst Mode" is enabled.
- Verify that Bluetooth audio feels "less hollow" when comb filtering mitigation is active.
