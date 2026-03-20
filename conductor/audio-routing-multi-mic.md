# EchoSense: Multi-Mic Array & Audio Routing Plan

## Background & Motivation
Currently, EchoSense defaults to whichever microphone the system provides (usually the wired headset if connected). To provide better flexibility and solve issues like clothing "scraping" on headset mics, we need to allow the user to manually select between the **Phone Mic** and the **Wired Headset Mic**.

## Scope & Impact
This task implements manual audio input routing.
It includes:
- Detecting available audio input devices (Built-in Mic vs. Wired Headset).
- Updating the UI with a Mic Selection toggle.
- Communicating with the Android `AudioManager` to preferred the selected input.
- Updating the `AudioEngine` to restart with the newly selected device ID if necessary.

## Implementation Steps

1. **Microphone Detection (`MainActivity.kt`):**
   - Use `AudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)` to list available mics.
   - Filter for `TYPE_BUILTIN_MIC` and `TYPE_WIRED_HEADSET`.

2. **UI Update (`activity_main.xml`):**
   - Add a "Microphone Source" `RadioGroup` or `Switch` above the visualizer.

3. **Routing Logic (`MainActivity.kt` & `EchoSenseService.kt`):**
   - Use `AudioManager.setCommunicationDevice()` (Android 12+) or `setPreferredDevice()` on the Oboe stream via JNI.
   - Implement a listener for device connection/disconnection events.

4. **JNI Updates (`native-lib.cpp` & `AudioEngine.h/cpp`):**
   - Add `setInputDevice(int deviceId)` to allow Oboe to target a specific hardware mic ID.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that you can switch between the Phone Mic and Headset Mic via the UI.
- Verify that the volume meter reflects the change in input source.
- Confirm that "scraping" noise is reduced when switching to the Phone Mic while wearing a headset.
