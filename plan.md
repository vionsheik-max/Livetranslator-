# Live Translator implementation plan

## Outcome
Build a calm Android live translation utility that captures the current screen after explicit user consent, recognizes foreign text, translates it on-device, and renders only translated text over the original locations. The floating control is a small translucent draggable orb; tapping it opens icon-only controls for stop, refresh, and minimize.

## Implementation
- Keep the existing Compose + multi-module structure.
- Use MediaProjection in a foreground service for screen capture, with Android 14+ `mediaProjection` foreground-service declarations.
- Use ML Kit Text Recognition, Language Identification, and on-device Translation. OCR regions are mapped from capture coordinates to display coordinates.
- Use frame-level change detection and an LRU translation cache. Only changed/new regions are translated; removed regions are absent from the next render frame.
- Render translations in a separate full-screen, non-touchable overlay window. The control window remains independent and draggable.
- Use Compose glass styling for the home experience and an icon-only control surface so no labels, symbols, or status text leak into the translation overlay.

## Design
- **Design movement:** quiet glassmorphism inspired by frosted water and soft dawn light.
- **Core principles:** low visual noise, motion that feels damped and intentional, strong hierarchy, and user-controlled privacy.
- **Color philosophy:** deep blue-black foundations reduce eye strain; mint and sky accents signal live state without aggressive saturation.
- **Layout paradigm:** a vertical breathing canvas of rounded glass cards, with the live orb acting as a movable anchor rather than a fixed toolbar.
- **Signature elements:** translucent circular Live Translate orb, thin luminous borders, and soft radial color bloom.
- **Interaction philosophy:** every control is reversible; expanded controls are compact and icon-first; drag gestures are smooth and clamped to the safe screen bounds.
- **Animation:** use subtle scale/fade transitions for panel expansion and avoid attention-grabbing pulses during translation.
- **Typography:** Material 3 typography with compact uppercase section labels and generous body leading.
- **Brand essence:** a quiet translation layer for everything already on your screen. Personality: calm, precise, unobtrusive.
- **Brand voice:** direct and reassuring, e.g. “Your screen, understood.” and “Translate in place, without the clutter.”
- **Wordmark/mark:** a rounded speech ripple enclosing a small horizon line, echoed by the orb.
- **Signature brand color:** Mist Mint `#7CF3D6`.

## Project structure
- `app`: Android permissions, Compose UI, overlay windows, foreground service, ML Kit adapters.
- `core`: platform-independent geometry, frame diffing, caching, and pipeline contracts.
- `plan.md`: approved implementation/design record.
