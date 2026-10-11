# Live Translator

Live Translator is an Android screen translation utility designed to stay out of the way. When the user explicitly starts a session, it captures the visible screen through MediaProjection, recognizes text with ML Kit, identifies the source language, translates changed regions on-device, and renders only the translated text back over the same screen coordinates.

## Product behavior

The floating control is a small translucent, draggable orb that stays clamped inside the display. Tapping it opens a compact glass panel with icon-first controls for refresh, minimize, and stop. The translation layer is a separate non-touchable window: it contains no status labels, controls, numbers, or explanatory text. Its text size is derived from the recognized source bounds, and the next frame removes overlays whose source text is no longer visible.

## Competitive direction

The repository includes an evidence-backed [competitive review](competitor-review.md). The product is intentionally not positioned as universally better than Google Translate, Google Lens, Papago, Microsoft Translator, or established screen-overlay apps. Its differentiation is a controlled source-aligned overlay, a future dedicated live-caption mode, explicit capability labeling, and privacy-transparent sessions.

## Live interpreter

The home screen exposes a separate Live Interpreter session. After microphone consent, `VoiceInterpreterService` listens for partial speech results, identifies the spoken language, translates with ML Kit on-device models, and renders only the translated caption text in a touch-through overlay. A foreground notification remains visible while listening. The current implementation is text-first; speaker separation, translated audio, transcript export, and device-specific offline guarantees are deliberately not overstated.

## Architecture

`app` owns Android permissions, MediaProjection, microphone speech recognition, the Compose home screen, foreground services, and overlay windows. `OnDeviceTranslationPipeline` connects ML Kit OCR, language identification, and on-device translation to the platform-independent contracts in `core`. `core` contains coordinate mapping, frame change detection, and an LRU translation cache so scrolling screens do not repeatedly translate unchanged text.

ML Kit translation models are downloaded on demand by the device. The first translation for a language pair may need network access for the model; subsequent translations run on-device. The app requests screen-capture and overlay permission only when the user turns on live translation. Microphone permission is declared for the planned voice interpreter/caption path, but it is not silently activated by the screen translation session.

## Build

Open the project in Android Studio with an Android SDK installed, or set `ANDROID_HOME`/`ANDROID_SDK_ROOT` and provide API 35. The project targets Java 17, Kotlin 2.0.21, AGP 8.7.3, and Gradle 8.10.2. From the repository root:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The repository snapshot used for implementation did not include a local Android SDK, so the final Android compile could not be executed in the sandbox. Core behavior remains covered by the existing unit tests; the first Android Studio/CI run should resolve any environment-specific SDK setup.
