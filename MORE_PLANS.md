1. Acoustic Safety & Core DSP (Must-Haves)
While you have frequency shaping and noise gating mapped out, hearing aids require specific safety and comfort fail-safes.

Acoustic Shock Protection (Brick-Wall Limiter): This is non-negotiable for user safety. If a user drops a pan or an ambulance drives by, your amplifier could output dangerous decibel levels. You need a fast-acting, look-ahead limiter at the very end of your C++ DSP chain to clamp output amplitudes instantly.

Acoustic Feedback Cancellation (AFC): Especially when you transition to Phase 4 (Pixel Buds Pro 2), sound bleeding from the earbud speaker back into the microphone will create a high-pitched howling loop. You need an adaptive filter (often using the LMS algorithm you mentioned for noise cancellation) specifically tuned to detect and invert phase-correlated feedback.

Wind Noise Reduction: Microphones on the ear or wrist are highly susceptible to wind rumble, which easily overloads the DSP and ruins transcription. Add an algorithmic toggle that detects the characteristic low-frequency, un-correlated rumbling of wind across multiple mics and aggressively rolls off the low-pass filter.

2. Clinical Personalization (Onboarding)
Currently, your plan relies on manual EQ sliders. To make this a true "hearing aid" app, automate the calibration.

In-App Pure-Tone Audiometry (Hearing Test): Build an onboarding flow that plays tones at standard audiological frequencies (250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz) at varying volumes.

Algorithmic Curve Generation: Once you capture the user's hearing thresholds, use an open-source audiology formula (like NAL-NL2) to automatically generate their baseline multiband compression and EQ curves, rather than making them guess with sliders.

3. Hardware Realities & Battery Constraints
Streaming continuous audio networks is heavily taxing, particularly on wearables.

WearOS "Burst" or "Push-to-Listen" Mode: The Pixel Watch 3 has a ~300-400mAh battery. Streaming continuous 48kHz audio via ChannelClient will kill the watch battery in under an hour. Instead of continuous streaming, implement a "Push-to-Target" UI on the watch face, or use accelerometer data (wrist raise) to trigger a 30-second burst of directional audio capture when the user leans in to hear someone.

Comb Filtering Mitigation (Phase 4): When you move to Bluetooth LE Audio, you will introduce roughly 20-40ms of latency. When the user's brain processes the natural sound leaking through the earbud passively alongside the delayed processed sound, it creates a hollow, robotic "comb filtering" effect. You will need to carefully tune your phase alignment or use transient masking to make the delay less jarring.

4. Android OS Integration
To make EchoSense feel native, tie it into the system's underlying accessibility framework.

Android Accessibility Service Integration: Register the app as an Accessibility Service. This allows you to float a persistent accessibility button on the screen for quick profile toggles (e.g., jumping from "Quiet Mode" to "Crowd Mode") without opening the app.

System Audio Ducking: Use Android's AudioFocusRequest carefully. If a phone call comes in or a system alarm goes off, your app needs a defined protocol to either duck its own DSP audio or pass the system audio through your EQ filters seamlessly.

5. Memory & Privacy
Biometric App Lock: Because EchoSense will store a Room DB full of highly personal, transcribed conversations, you should implement Android's BiometricPrompt. Require a fingerprint or face scan to access the "Memory" and "Transcript" history tabs to protect user privacy if the phone is handed to someone else.
