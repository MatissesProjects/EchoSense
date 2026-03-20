# EchoSense: Pixel Watch 3 Integration & Bluetooth ANC Control Plan

## Background & Motivation
Phase 2.2 focuses on extending the microphone array to the user's wrist using the Pixel Watch 3 (WearOS). Additionally, as we move towards wireless I/O (Phase 4), we need to handle the latency introduced by Bluetooth by allowing the user to enable Active Noise Canceling (ANC) on their buds, which makes the small processing delay feel more natural.

## Scope & Impact
This task implements the foundational link between the phone and the watch and adds Bluetooth capability controls.
It includes:
- **WearOS Link:**
    - Setting up the WearOS Data Layer / Message Client.
    - Creating a skeleton WearOS app project (handled in next steps).
    - Implementing a "Watch Mic" listener on the phone to receive remote PCM data.
- **Bluetooth Optimization:**
    - Detecting Bluetooth headset connection.
    - Adding a UI toggle for "ANC Mode" (Active Noise Canceling).
    - Communicating with the `AudioManager` to request ANC/Transparency modes via Bluetooth standard APIs (where supported).
- **Audio Mesh Update:**
    - Updating the C++ `AudioEngine` to accept an external PCM buffer (from the watch) as an alternative input source.

## Implementation Steps

1. **Bluetooth Mode Control (`MainActivity.kt`):**
   - Implement `AudioManager` logic to detect and control Bluetooth headset modes.
   - Add a "Wireless Optimization" section to the UI with an ANC toggle.

2. **WearOS Data Layer Setup (Phone side):**
   - Add `play-services-wearable` dependency.
   - Implement `WearableListenerService` on the phone to receive incoming audio packets from the Watch.

3. **C++ Remote Input Bridge (`AudioEngine.h/cpp`):**
   - Add `setRemoteInput(float* data, int size)` method.
   - Add a `mInputSource` enum (Built-in, Headset, Watch).
   - Update `onAudioReady` to pull from the remote buffer when "Watch" is selected.

4. **UI Layout Updates (`activity_main.xml`):**
   - Add "Watch" to the Microphone Source selection.
   - Add the Bluetooth ANC toggle.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that the Bluetooth ANC toggle appears when Buds Pro 2 are connected.
- Verify the phone can successfully register as a WearOS Data Layer listener.
- (Manual) Verify that audio processing continues smoothly even with the additional data layer overhead.
