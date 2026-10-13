# Live Translator

Live Translator is an Android screen translation utility designed to stay out of the way. When the user explicitly starts a session, it captures the visible screen through MediaProjection, recognizes text with ML Kit, identifies the source language, translates changed regions on-device, and renders only the translated text back over the same screen coordinates.

## Product behavior

The floating control is a small translucent, draggable orb that stays clamped inside the display. Tapping it opens a compact glass panel with icon-first controls for refresh, minimize, and stop. The translation layer is a separate non-touchable window: it contains no status labels, controls, numbers, or explanatory text. The capture loop refreshes every 180 ms, processes every visible OCR line, caches unchanged translations, maps each result to the latest source bounds, and removes overlays as soon as their source line leaves the frame.

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

The sandbox build has been validated with API 35 and Java 17. The debug APK is assembled by the same command above.

## Monetization

The app now includes a real Google Play Billing one-time product flow. The product ID is `live_translator_pro`. The free tier keeps basic screen translation available. Pro unlocks the voice interpreter and offline language-pack downloads, while removing ads and recurring fees from the product promise.

Before publishing, create an **in-app product** with ID `live_translator_pro` in Play Console, configure its localized name and price, add the app to an internal testing track, and install the Play-distributed build. A locally sideloaded debug APK cannot complete a real Play purchase.

The app queries localized product details, restores owned purchases, acknowledges completed purchases, and exposes a clear upgrade card in Settings. For a commercial release, the purchase token should also be verified on a secure server through the Google Play Developer API before granting long-lived entitlements; the current client-side flow is suitable for the first internal test track, not a substitute for server-side fraud protection.

## Performance guarantees in the current release

The live loop now paces from the start of capture, so OCR work cannot create a backlog on a slow phone. A cheap visual fingerprint skips OCR and translation work for unchanged frames. OCR regions are deduplicated before language detection, repeated source-language lookups are cached, and ML Kit model downloads are performed once per language pair. Independent translation lines no longer share one global lock, and the translation overlay skips redundant Compose recompositions when its text and bounds have not changed.

These are code-level protections rather than a claim that every phone will deliver a fixed frame rate. Before a public launch, benchmark at least a budget, mid-range, and flagship Android device for translation latency, battery use, heat, dropped frames, and scrolling/video subtitle stability.

## Runtime reliability correction

The corrective release isolates each optional ML Kit script recognizer. If a Chinese, Japanese, Korean, or Devanagari model is unavailable, the healthy recognizers continue instead of `awaitAll()` failing the entire OCR frame. The service no longer converts OCR exceptions into a silent empty overlay: the home screen reports whether the session is starting, scanning with no foreign text, translating visible lines, or receiving a capture/OCR error. Granting overlay permission also continues automatically into the screen-capture prompt rather than requiring a second toggle.

The repository build and tests pass, but a physical Android-device run is still required to prove MediaProjection and vendor-specific overlay behavior. The new runtime status is intentionally there to make that final test observable instead of guessing from a blank screen.
