# Live Translator implementation plan

## Competitive verdict

Live Translator should not claim to beat every competitor. Google Translate and Google Lens are stronger in broad multimodal coverage, Papago is strong for two-person voice and Asian-language context, and Instant Translate On Screen already validates floating screen overlays. The defensible product position is a calm Android layer that combines reliable source-aligned screen translation with live captions, explicit permission and capability labeling, and privacy-transparent processing.

## Product outcome

Build an Android live translation utility that captures the current screen only after explicit consent, recognizes foreign text, translates changed regions, and renders only translated text over the original locations. The floating control is a small translucent draggable orb; tapping it opens icon-only controls for stop, refresh, and minimize.

The product has two separate session modes rather than one vague promise. **Screen Translate** handles persistent OCR overlays. **Live Captions** handles microphone speech as a text-first stream with explicit source/target language, pause/stop, font-size, online/offline state, and optional translated audio. A later **Conversation** mode can add two-speaker turn taking without weakening the simpler caption workflow.

## Implementation priorities

### P0 — dependable core loop

Use MediaProjection in a foreground service for screen capture, with Android 14+ `mediaProjection` declarations. Use ML Kit Text Recognition for Latin, Chinese, Japanese, Korean, and Devanagari scripts, plus Language Identification and on-device Translation. Map OCR regions from capture coordinates to display coordinates. Use frame-level change detection and an LRU translation cache so unchanged regions are not repeatedly translated and removed source regions disappear immediately. Capture refreshes every 180 ms, processes every OCR line returned in the visible frame, caches language identification, deduplicates translation work across repeated lines, and re-renders each cached translation at the latest OCR bounds so it follows scrolling content.

Render translations in a separate full-screen, non-touchable overlay window. The translation layer contains no status labels, controls, numbers, or explanatory text. The control window remains independent and draggable. Define measurable device-based targets for capture-to-render latency, refresh cadence, non-overlap, and stale-overlay removal instead of promising an unverified universal speed.

Before a session starts, show a capability contract: required permissions, source/target pair, whether a model download is needed, whether processing is local or remote, and known device/language limitations. During a session, show only the system-required foreground notification and the translation text overlay.

### P1 — quality and trust

Add an in-app fallback when overlay or capture permission is unavailable. Add source-only/translation-only toggles, crop/region selection, freeze/retry, original-text visibility, confidence or quality guidance, and layout logic that prevents overlapping translated windows. Handle small, stylized, handwritten, patterned, vertical, moving, and mixed-language text with explicit retry guidance rather than silent confidence.

Add a privacy contract for every sensitive path. Disclose when screen capture, microphone, copied text, or image data is active; state when a model download or remote translation is required; add a history/retention toggle and deletion action; and keep analytics/diagnostics separate from translation data. Do not call a path local-only until it is verified on the target device and language pair.

Add downloadable model packs for the highest-value language pairs with storage management, pack version/status, graceful online fallback, and a per-mode capability matrix. Offline support must be labeled by mode, device, Android version, RAM, and language pair.

### P1 — live captions

Implemented in the current release as a dedicated microphone foreground service. The service requests microphone permission from the user, uses Android SpeechRecognizer partial results, identifies the spoken language, downloads the required ML Kit translation model when needed, and renders translated captions in a separate non-touchable overlay. The session has an explicit on/off lifecycle and a system-visible foreground notification. It is intentionally text-first; speaker separation, translated audio, headset routing, and transcript export remain follow-up work. Keep it separate from screen OCR so a caption failure cannot stop the screen overlay.

### P2 — utility and breadth

After the live loop is stable, add transcript save/search/copy/share/export/replay and adjustable typography. Add image/screenshot import, selection, and original-versus-translation inspection. Expand language breadth and advanced modes only after device testing identifies demand; quality and truthful support matter more than headline language counts.

## Design

- **Design movement:** quiet glassmorphism inspired by frosted water and soft dawn light.
- **Core principles:** low visual noise, motion that feels damped and intentional, strong hierarchy, and user-controlled privacy.
- **Color philosophy:** deep blue-black foundations reduce eye strain; mint and sky accents signal live state without aggressive saturation.
- **Layout paradigm:** a vertical breathing canvas of rounded glass cards, with the live orb acting as a movable anchor rather than a fixed toolbar.
- **Signature elements:** translucent circular Live Translate orb, thin luminous borders, and soft radial color bloom.
- **Interaction philosophy:** every control is reversible; expanded controls are compact and icon-first; drag gestures are smooth and clamped to safe display bounds.
- **Animation:** use subtle scale/fade transitions for panel expansion and avoid attention-grabbing pulses during translation.
- **Typography:** Material 3 typography with compact uppercase section labels and generous body leading.
- **Brand essence:** a quiet translation layer for everything already on your screen. Personality: calm, precise, unobtrusive.
- **Brand voice:** “Your screen, understood.” and “Translate in place, without the clutter.”
- **Wordmark/mark:** a rounded speech ripple enclosing a small horizon line, echoed by the orb.
- **Signature brand color:** Mist Mint `#7CF3D6`.

## Project structure

- `app`: Android permissions, Compose UI, overlay windows, foreground service, ML Kit adapters, and future caption pipeline.
- `core`: platform-independent geometry, frame diffing, caching, layout rules, and pipeline contracts.
- `competitor-review.md`: evidence-backed competitor findings and product implications.
- `plan.md`: benchmark-informed implementation and design record.
