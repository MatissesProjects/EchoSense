# EchoSense: High-Gain Master Boost & UI Polish Plan

## Background & Motivation
The current audio output is too quiet. We need a significantly higher gain range for the Master Boost (Band 1) to ensure the microphone output is clearly audible, especially for the hearing assistant use case.

## Scope & Impact
This task increases the available gain in the C++ engine and updates the UI scaling in Kotlin.
It includes:
- Updating `AudioEngine.cpp` to support a higher gain multiplier (e.g., up to 10.0x or 20.0x).
- Updating `MainActivity.kt` to scale the Master Gain slider (Band 1) appropriately.
- Adding a "Boost Factor" label to the UI for better feedback.

## Implementation Steps

1. **Update C++ Engine (`AudioEngine.cpp`):**
   - Increase the gain multiplier logic in `processSample`.

2. **Update Kotlin UI Logic (`MainActivity.kt`):**
   - Change `Band 1` scaling from `0.0-2.0` to `0.0-10.0` (or higher).
   - Update the UI label to show "Master Gain (High Boost)".

3. **Update UI Layout (`activity_main.xml`):**
   - Ensure Band 1 is clearly labeled as the Master/Boost control.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that sliding Band 1 to the right provides a significant volume increase.
- Monitor the volume meter to ensure no digital clipping (though Oboe/Android might handle some limiting).
