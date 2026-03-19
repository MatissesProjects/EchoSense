# EchoSense: Initial Project Setup Plan

## Background & Motivation
According to the `PLAN.md`, Phase 1 of EchoSense requires building a wired "zero-latency" foundation. To achieve this, we need an Android application configured with C++ (NDK) support to utilize Google's Oboe library for low-latency audio processing. This initial setup is the prerequisite for all subsequent audio routing and DSP tasks.

## Scope & Impact
This plan covers the creation of the foundational Android project structure. 
It includes:
- Initializing a Kotlin-based Android project.
- Configuring the Android NDK and CMake for C++ native code support.
- Integrating the Google Oboe library for audio processing.
- Creating the initial JNI (Java Native Interface) bridge to allow Kotlin UI to communicate with the C++ audio engine.
- Setting up the required permissions (e.g., `RECORD_AUDIO`).

## Proposed Solution
We will use standard Android build tools (Gradle) to set up a hybrid Kotlin/C++ project. Oboe will be integrated via Prefab (Android's native dependency management system).

## Implementation Steps

1. **Scaffold Android Project:**
   - Create the standard Android directory structure (`app/src/main/java`, `app/src/main/res`, etc.).
   - Create `AndroidManifest.xml` with `<uses-permission android:name="android.permission.RECORD_AUDIO" />`.

2. **Configure Gradle Build System:**
   - Create root `build.gradle.kts` and `settings.gradle.kts`.
   - Create `app/build.gradle.kts` with:
     - Kotlin Android plugin.
     - NDK and CMake configuration.
     - Oboe dependency (`implementation("com.google.oboe:oboe:1.8.0")`) with `buildFeatures { prefab = true }`.

3. **Set Up Native Environment (C++):**
   - Create `app/src/main/cpp/CMakeLists.txt` to define the native library, find the Oboe package, and link it.
   - Create an initial `app/src/main/cpp/AudioEngine.cpp` and `AudioEngine.h` with basic Oboe stream setup structures.
   - Create `app/src/main/cpp/native-lib.cpp` to serve as the JNI entry point.

4. **Set Up Kotlin Application:**
   - Create `MainActivity.kt` that loads the native library (`System.loadLibrary("echosense_native")`).
   - Implement basic lifecycle methods to start/stop the audio engine via JNI.

## Verification
- Run a Gradle sync to ensure all dependencies (including NDK and Oboe) resolve correctly.
- Execute `./gradlew assembleDebug` to verify that both the Kotlin and C++ code compile and link successfully.
- Ensure no build errors regarding Prefab or missing native libraries.