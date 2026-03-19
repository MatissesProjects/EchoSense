# EchoSense: On-Device AI Transcription & Live Captions Plan

## Background & Motivation
Phase 3 of EchoSense aims to provide real-time visual assistance by transcribing the cleaned-up speech audio. Since we are targeting the Google Pixel 9 Pro XL, we want to leverage on-device AI for transcription and eventual summarization.

## Scope & Impact
This task focuses on the first milestone of the Intelligence phase: Live Transcription.
It includes:
- Integrating Android's `SpeechRecognizer` (on-device mode).
- Updating the UI to include a live caption display area.
- Implementing a mechanism to feed the processed audio into the transcription engine (or using the system's capture context).
- Setting up the foundational AICore integration for future Gemini Nano summarization.

## Implementation Steps

1. **Update UI Layout (`activity_main.xml`):**
   - Add a `TextView` or a dedicated "Live Caption" container at the bottom of the UI.
   - Style it for high readability (black background, large white text).

2. **Integrate Speech-to-Text (`MainActivity.kt`):**
   - Initialize `SpeechRecognizer` with `Intent.ACTION_RECOGNIZE_SPEECH`.
   - Set `EXTRA_PREFER_OFFLINE` to true to ensure on-device processing.
   - Implement `RecognitionListener` to handle partial and final results.

3. **Audio Routing for Transcription:**
   - Note: Android's `SpeechRecognizer` typically uses its own microphone management. We will first implement it using the standard system capture to verify on-device performance, then explore feeding our cleaned DSP audio if necessary (Phase 3.2).

4. **Prepare AICore (Foundational):**
   - Add the necessary dependencies for Google AICore / Gemini Nano to `build.gradle.kts`.
   - Verify AICore availability on the Pixel 9 Pro XL.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that spoken words appear on the screen in real-time.
- Verify that transcription works even in Airplane Mode (confirming on-device processing).
