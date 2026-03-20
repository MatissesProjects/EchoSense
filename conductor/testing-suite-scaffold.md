# EchoSense: Comprehensive Testing Suite Plan

## Background & Motivation
As the project grows in complexity (DSP, JNI, Background Services, AI, WearOS), we need a robust testing suite to ensure that core audio processing and application logic remain functional and glitch-free.

## Scope & Impact
This plan covers unit and integration tests across all layers of the application.
It includes:
- **C++ Native Tests:** Verifying Biquad filter responses, Gain/Limiter logic, and Ring Buffer stability.
- **Kotlin Unit Tests:** Verifying lifecycle states, permission handling, and message processing.
- **JNI Integration Tests:** Ensuring the bridge correctly maps types and handles null/invalid states.

## Implementation Steps

1. **Native C++ Unit Tests (`app/src/test/cpp`):**
   - Create a standalone test runner for the `Biquad` class.
   - Verify that High-Pass filters actually attenuate low frequencies.
   - Verify that the Limiter prevents samples from exceeding +/- 1.0.

2. **Kotlin Unit Tests (`app/src/test/java`):**
   - Test `MainActivity` state transitions.
   - Test `EchoSenseService` foreground notification creation.
   - Mock `MessageClient` to verify WearOS data parsing.

3. **CI/CD Foundation:**
   - Configure `./gradlew test` to run both Kotlin and (eventually) Native tests.

## Verification
- Run `./gradlew test` and ensure 100% pass rate.
- Manual verification of APK on-device (once reconnected).
