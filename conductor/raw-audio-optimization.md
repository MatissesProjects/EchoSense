# EchoSense: Raw Audio Capture & Full-Duplex Optimization Plan

## Background & Motivation
The user reports that only background noise and scraping are audible, while speech is faint. This suggests that Android's default audio processing (noise suppression/AGC) might be aggressively filtering out speech or that the input/output routing is not optimized for raw, low-latency passthrough.

## Scope & Impact
This task focuses on obtaining the "rawest" possible audio from the microphone and optimizing the playback path.
It includes:
- Configuring the `InputPreset` to `Unprocessed` (or `VoiceRecognition`) to bypass Android's system-level filters.
- Setting the `Usage` and `ContentType` for the playback stream.
- Improving the Full-Duplex logic to reduce potential data loss during passthrough.
- Increasing the `MasterGain` limit to 50x (+34dB) for extreme amplification cases.

## Implementation Steps

1. **Update C++ Engine (`AudioEngine.cpp`):**
   - Set `builder.setInputPreset(oboe::InputPreset::Unprocessed)` for the recording stream.
   - Set `builder.setUsage(oboe::Usage::VoiceCommunication)` and `builder.setContentType(oboe::ContentType::Speech)` for the playback stream.
   - Adjust the `onAudioReady` callback to handle the output write more robustly.

2. **Update Kotlin UI (`MainActivity.kt`):**
   - Scale the `Master Gain` slider to support a 0.0x to 50.0x range.
   - Update the UI label to emphasize "Speech Boost."

3. **Verify Configuration:**
   - Ensure the `SampleRate` is being correctly requested from the system's optimal defaults.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify if speech becomes significantly clearer and louder.
- Verify if the "scraping" noise is still dominant (if so, we may need to investigate mic selection logic).
