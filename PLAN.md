# Project Plan: EchoSense (Adaptive Hearing & Memory App)

## 1. Executive Summary
EchoSense is an Android-first application designed to act as an intelligent, contextual hearing assistant. It captures ambient audio via a dynamic array of device microphones (Phone, Watch, Headset), isolates and boosts human speech, and utilizes on-device AI to transcribe, summarize, and remember conversation contexts.

## 2. Target Hardware & Ecosystem
* **Primary Processing Device:** Google Pixel 9 Pro XL (Leveraging Tensor G4 for on-device ML/DSP).
* **Phase 1 Audio I/O (Zero Latency):** Original Google Pixel Wired USB-C Buds.
* **Phase 2 Wireless I/O:** Google Pixel Buds Pro 2 (BT LE Audio).
* **Tertiary Sensor/Mic:** Google Pixel Watch 3 (WearOS).

## 3. Core Features

### 3.1. Dynamic Audio Routing & DSP Array
* **Intelligent Mic Selection:** * *Wired Headset Mic:* Primary capture for the user's own voice and immediate frontal audio.
    * *Phone Mic (Desk/Pocket Mode):* Broad ambient capture or directional pointing (if on a desk).
    * *Watch Mic:* On-demand directional capture (e.g., raising your wrist to catch a specific voice in a noisy room).
* **Zero-Latency Passthrough:** Utilizing wired USB-C connections for Phase 1 to perfect the DSP without Bluetooth interference.
* **Real-Time Voice Isolation:** Filtering background noise while preserving human vocal frequencies.
* **Custom EQ Boosting:** Tailoring frequency amplification to the user's needs.

### 3.2. Contextual Transcription & Note-Taking
* **Live Local Transcription:** Real-time speech-to-text via Android's `SpeechRecognizer` or local Whisper models.
* **Speaker Diarization:** Distinguishing between Speaker A (User) and Speaker B (Interlocutor).
* **Auto-Summarization:** Condensing conversations using on-device LLMs (Google AICore / Gemini Nano).

### 3.3. "Memory" & Persona Tracking
* **Profile Tagging:** Associating transcripts with specific individuals.
* **Context Recall:** Surfacing past conversational notes dynamically.
* **Secure Local History:** Encrypted, on-device database of interactions.

## 4. Technical Architecture

### 4.1. The Audio Mesh Pipeline
* **API Core:** Google's **Oboe** C++ library for low-latency audio processing.
* **Multi-Source Routing:** Logic to handle Android `AudioManager` states, switching dynamically between the wired headset mic, the device's internal mics, and incoming audio streams from the WearOS companion app.
* **DSP Engine:** Custom C++ filtering for noise gating and EQ bands applied *before* audio is routed to the headphones.

### 4.2. AI & Data Pipeline
* **Summarization:** Google AICore API for localized, private text summarization.
* **Storage:** Room Database (SQLite) for managing user profiles, conversation transcripts, and metadata.

## 5. Development Phases

### Phase 1: The Wired "Zero-Latency" Foundation
* [x] Setup Android project with C++ (NDK) support.
* [x] Implement Oboe for direct USB-C Mic -> USB-C Headphone audio passthrough.
* [x] Build basic DSP: Implement a noise gate and a 5-band EQ slider in the UI.
* [x] Test latency and ensure audio is perfectly synced.

### Phase 2: Multi-Mic Array & WearOS Link
* [x] Implement `AudioManager` routing to allow switching between the Phone Mic and the Headset Mic via the UI.
* [x] Develop WearOS companion app.
* [ ] **WearOS Data Stability:** Resolve `ChannelClient` streaming issues (Currently tabled).
* [x] Implement DSP Serial Routing, Profiles (Voice/Music/TV), and Noise Gate Hysteresis.
* [x] **Persistent Customization:** Implement Manual EQ offsets that persist across profile changes.
* [x] **Target Lock (Crowd Mode):** Implement hyper-restrictive bandpass filtering to isolate human speech fundamentals in extreme noise.
* [x] **Speaker Isolation (AI Prototype):** Added UI and DSP hooks to isolate specific speakers (Watch vs Phone).

### Phase 3: Intelligence & Transcription
* [x] Integrate on-device Speech-to-Text.
* [x] Feed the filtered DSP audio into the transcription engine.
* [ ] Implement Google AICore (Gemini Nano) to summarize the resulting text.

### Phase 4: Stabilization & Memory
* [ ] **Sensor Fusion Stabilization:** Debug and fix WearOS 16kHz->48kHz resampling and streaming data flow.
* [x] Set up the Room Database for profiles and conversational history.
* [ ] Build the UI for viewing past notes.
* [ ] *The Wireless Test:* Introduce the Pixel Buds Pro 2 and attempt to apply the Phase 1 DSP code to the Bluetooth LE Audio stream, optimizing buffer sizes to mitigate the newly introduced latency.

### Phase 5: Intelligent Scene Awareness
* [ ] **Adaptive Scene Detection:** Automatically switch to "Music/Quiet" when ambient noise is detected and "Voice" when speech harmonics are identified.
* [ ] **Priority-Based Profiles:** Implement a hierarchy (Speech > Ambient > Music) for automated profile transitions.
* [ ] **Environmental Fingerprinting:** Use FFT data to create and recognize unique acoustic signatures of different spaces.

### Phase 6: Spectral AI & Advanced DSP
* [x] **True FFT Integration:** Integrate a lightweight FFT library (custom implementation) into the C++ engine.
* [x] **Spectral Subtraction:** Implement a "Learn Noise" feature that subtracts the frequency-domain noise floor.
* [x] **Neural Multi-band Gate:** Implement 64-bin spectral gating to isolate vocals from overlapping background noise.
* [x] **Gemini Nano (AICore) Summarization:** Use the Pixel 9's on-device LLM to summarize conversation notes stored in the Room DB.

## 6. Known Risks & Mitigations
1.  **Audio Stream Collisions:** Android limits how many apps/services can access the mic simultaneously. Mitigated by carefully managing the `AudioRecord` lifecycle and building a robust internal audio mixer.
2.  **Battery Drain:** Mitigated by offering a "Passive Mode" (audio boost only) and an "Active Mode" (audio + transcription + AI).
3.  **Privacy:** Mitigated by strictly keeping all ML and audio processing on-device.
