# EchoSense: On-Device Gemini Nano Summarization Implementation

## Background & Motivation
The original `PLAN.md` emphasizes privacy and low-latency on-device processing. Currently, the `SummarizationManager` uses the cloud-based `generativeai` SDK or a mock simulator. To fulfill the Phase 6 goals on the Pixel 9 Pro XL, we need to utilize **Gemini Nano via Google AICore**.

## Scope & Impact
This task focuses on the integration of on-device LLM for conversation summarization.
It includes:
- Updating `app/build.gradle.kts` with the necessary AICore / Vertex AI dependencies.
- Implementing an on-device summarization provider in `SummarizationManager.kt`.
- Ensuring that conversations stored in the Room DB are processed efficiently by the local model.
- Handling model availability checks (e.g., ensuring the device is compatible and the model is downloaded).

## Implementation Steps

1. **Dependency Update (`app/build.gradle.kts`):**
   - Add the Google Vertex AI SDK for Android (which supports on-device Gemini Nano).
   - `implementation("com.google.ai.client.generativeai:generativeai:0.9.0")` (or latest stable supporting AICore).

2. **Refine `SummarizationManager.kt`:**
   - Create a `GeminiNanoSummarizer` class that initializes the local model via `GenerativeModel(modelName = "gemini-nano")`.
   - Update `summarizeAndSave` to use this local provider instead of the simulated mock.
   - Implement error handling for cases where AICore is not yet initialized or the model needs downloading.

3. **Context Optimization:**
   - Develop a specialized prompt for Gemini Nano that is optimized for short-to-medium conversation fragments.
   - Ensure the "Speaker A (Watch)" vs "Speaker B (Phone)" distinction is clear to the LLM.

4. **UI Feedback:**
   - Update `MainActivity` to show a "Model Downloading..." or "AI Initializing..." status if the local model isn't ready.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Deploy to Pixel 9 Pro XL.
- Verify summarization works in **Airplane Mode**.
- Confirm that no "TODO_USER_API_KEY" is required for local summarization.
