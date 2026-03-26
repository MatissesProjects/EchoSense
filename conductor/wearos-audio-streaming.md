# EchoSense: WearOS 16kHz Audio Streaming Implementation

## Background & Motivation
The `PLAN.md` for Phase 2.2 and 4 requires the Pixel Watch 3 to act as a remote microphone. While the phone side (`EchoSenseService`) is already prepared to receive a PCM stream via `ChannelClient`, the watch-side implementation (`WearSensorService`) currently lacks any audio capture or streaming logic. This is critical for spatial beamforming and "Watch Focus" mode.

## Scope & Impact
This task focuses on the watch-side implementation of audio streaming.
It includes:
- Adding `RECORD_AUDIO` and `FOREGROUND_SERVICE_MICROPHONE` permissions handling on the watch.
- Implementing `AudioRecord` at 16kHz (mono, 16-bit PCM) in `WearSensorService`.
- Using `ChannelClient` to open an output stream to the phone.
- Implementing a "Burst" mode to preserve battery by only streaming when requested.

## Implementation Steps

1. **Watch Permissions & Service Update (`wear/src/main/AndroidManifest.xml`):**
   - Ensure permissions are present (already added in research phase).
   - Update `WearSensorService` to properly handle the microphone foreground type.

2. **Watch Audio Capture (`wear/src/main/java/com/echosense/wear/WearSensorService.kt`):**
   - Initialize `AudioRecord` with 16000Hz, `CHANNEL_IN_MONO`, `ENCODING_PCM_16BIT`.
   - Create a background loop (Coroutines) to read audio chunks.

3. **Watch Streaming Logic (`WearSensorService.kt`):**
   - Use `Wearable.getChannelClient(context).openChannel(nodeId, "/audio_stream_channel")` to connect to the phone.
   - Write PCM bytes from the `AudioRecord` buffer directly into the channel's `OutputStream`.

4. **Watch UI Integration (`wear/src/main/java/com/echosense/wear/MainActivity.kt`):**
   - Add a toggle button to "Start Streaming" or "Enable Watch Mic".
   - Communicate with `WearSensorService` to start/stop the capture loop.

5. **Phone-Side Refinement (`app/src/main/java/com/echosense/app/EchoSenseService.kt`):**
   - Verify that the `receiveAudioStream` logic correctly handles the incoming 16kHz stream.
   - (Optional) Ensure the `AudioConverter.pcmToFloat` and `AudioEngineLib.writeRemoteAudio` are efficient enough for the Pixel 9.

## Verification
- Run `./gradlew wear:assembleDebug`.
- Deploy to Pixel Watch 3 and Pixel 9 Pro XL.
- Verify the phone logs "Read X bytes on phone..." from the `receiveAudioStream` method.
- Use the "Watch" mic source in the phone UI and confirm audio is processed.
