# EchoSense: Audio Stability & Glitch Fix Plan

## Background & Motivation
The user reports "crackling" and "cutting off" in the audio. This is caused by two main issues in the current C++ engine:
1.  **CPU Overload:** The naive DFT (Discrete Fourier Transform) used for visualization is $O(N^2)$, which is far too slow for the high-priority real-time audio thread.
2.  **Clock Mismatch:** Writing directly to the output stream from the input callback causes jitter because the hardware clocks for input and output are never perfectly synchronized.

## Scope & Impact
This task focuses on making the audio engine production-ready and glitch-free.
It includes:
- Moving visualization analysis to a background thread.
- Implementing a **Ring Buffer (FIFO)** to decouple input and output streams.
- Switching to a dual-callback architecture (Oboe's recommended Full-Duplex pattern).
- Optimizing `processSample` for maximum speed.

## Implementation Steps

1. **Implement Thread-Safe FIFO (`AudioEngine.h/cpp`):**
   - Use a simple lock-free ring buffer or a mutex-protected buffer to move data between the input and output callbacks.

2. **Dual-Callback Architecture:**
   - Create a second callback class for the `PlaybackStream`.
   - Recording Callback: Process samples -> Push to FIFO.
   - Playback Callback: Pull from FIFO -> Write to hardware.

3. **Asynchronous Visualization:**
   - Move the `processFft` logic into a separate `std::thread` or trigger it only occasionally using a low-priority flag.
   - For now, we will simplify the visualization to avoid $O(N^2)$ calculations.

4. **Optimization:**
   - Ensure all math operations are using `f` suffixes (e.g., `sinf`, `cosf`) to avoid double-precision conversion.
   - Verify `std::clamp` is used efficiently.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy and verify that the "crackling" is completely gone.
- Verify that the volume meter and visualizer still function (even if slightly delayed).
- Verify that the total latency remains low enough for "zero-latency" feel.
