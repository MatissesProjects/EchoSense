# EchoSense AI - Intelligent Adaptive Hearing Assistant

EchoSense is an advanced, low-latency Android application designed to act as a contextual hearing assistant. It leverages an on-device C++ DSP engine and Wear OS multi-mic array to isolate speech, enhance clarity, and provide real-time transcription.

## Key Features

- **Low-Latency C++ Audio Engine:** Powered by [Google Oboe](https://github.com/google/oboe) for high-performance, real-time audio processing.
- **Multi-Mic Array:** Dynamically switch between Phone, Wired Headset, and **Wear OS (Watch)** microphones.
- **Intelligent DSP Pipeline:**
    - **Serial Routing:** Optimized processing path (Voice Filters → Graphic EQ) to prevent gain spikes.
    - **Soft-Knee Noise Gate:** Includes a 50ms temporal hold (hysteresis) to prevent audio "chattering."
    - **Remote AI Boost:** Dedicated gain control for the Wear OS microphone to compensate for distance.
- **Audio Profiles:** Instantly switch between **Voice**, **Music**, and **TV** optimized settings.
- **Live AI Transcription:** On-device speech-to-text with persistent foreground processing.
- **Stealth Mode:** Dimmed screen and "Keep Screen On" options for discreet, continuous use.

## Tech Stack

- **Android NDK (C++17):** Real-time DSP and filter implementation.
- **Kotlin:** App logic, Foreground Services, and UI.
- **Wear OS Data Layer:** High-speed PCM audio streaming from Watch to Phone.
- **Google Oboe:** Low-latency AAudio/OpenSL ES wrapper.
- **Material Design 3:** Modern, dark-themed interface for accessibility.

## Build & Installation

### Prerequisites
- **JDK 24+** (Required for the current build configuration).
- **Android SDK & NDK (v27+).**
- **ADB** enabled on both Phone and Watch.

### Building from Source
Set your `JAVA_HOME` to a JDK 24+ installation:

```powershell
$env:JAVA_HOME = 'C:\Path\To\JDK24'
./gradlew assembleDebug
```

### Deploying to Devices
Connect your devices via ADB (Wireless Debugging recommended for Watch):

```powershell
# Install to Phone
adb -s <phone_ip> install -r app/build/outputs/apk/debug/app-debug.apk

# Install to Watch
adb -s <watch_ip> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

## Roadmap

- **Phase 4:** Room Database integration for conversation history and "Memory" profiles.
- **Phase 5 (Intelligent Scene Awareness):**
    - **Adaptive Scene Detection:** Automatic switching based on acoustic fingerprinting.
    - **Priority Profiles:** Speech detection priority over ambient music.
