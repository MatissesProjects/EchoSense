# EchoSense: Jetpack Compose UI Migration Plan

## Background & Motivation
Modern Android applications are moving away from traditional XML View-based layouts toward **Jetpack Compose**. Migrating EchoSense to Compose will enable a more reactive UI, easier state management for the DSP parameters, and smoother animations for the audio visualizations.

## Scope & Impact
This task focuses on a phased migration of the `app` module's UI.
It includes:
- Updating `app/build.gradle.kts` with Compose dependencies.
- Implementing a `Theme.kt` with EchoSense branding (Dark Mode priority).
- Creating Composable components for the DSP sliders (EQ, Gain, Thresholds).
- Implementing a Compose-based `NotesHistoryActivity` with lazy lists and better swipe actions.
- Integrating the existing `FrequencyVisualizerView` into a Compose `AndroidView` wrapper.

## Implementation Steps

1. **Dependency Update:**
   - Add `compose-bom`, `ui`, `material3`, `activity-compose`, and `lifecycle-viewmodel-compose`.
   - Enable `buildFeatures { compose = true }`.

2. **Core Theme & Components:**
   - Define a custom `EchoSenseTheme` with high-contrast colors suitable for users with visual impairments.
   - Create a reusable `ParamSlider` component with label, value display, and haptic feedback.

3. **DSP Dashboard Migration:**
   - Rebuild the `MainActivity` layout using a `Scaffold` and a `VerticalScroll` container.
   - Use `collectAsStateWithLifecycle` to bind `AudioSettingsManager` values to the UI.

4. **Conversation History Migration:**
   - Convert `NotesHistoryActivity` to a `LazyColumn` for smoother scrolling and dynamic summarization updates.

## Verification
- Run `.\gradlew.bat assembleDebug`.
- Verify UI parity with the existing XML layouts.
- Confirm that DSP parameters still update the C++ engine in real-time.
