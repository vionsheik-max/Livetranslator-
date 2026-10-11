# Live Translator delivery checklist

- [ ] The app is named Live Translator and provides a soft, calming Compose home experience.
- [ ] Before a session starts, the home screen explains permissions, model downloads, local processing scope, and offline limitations.
- [ ] When live translation is enabled, a small translucent circular control appears above other apps.
- [ ] The circular control can be dragged smoothly and remains clamped inside the display.
- [ ] Tapping the control opens a compact translucent interface with stop, refresh, and minimize controls.
- [ ] Minimize returns to the original draggable circular control.
- [ ] Translation overlays show only translated text, with no labels, icons, symbols, or numbers in the translation layer.
- [ ] Translations are positioned from OCR bounds, use a text size derived from the source text bounds, and disappear when the source region disappears.
- [ ] Screen frames are processed through OCR, language identification, translation, caching, and coordinate mapping.
- [ ] Android 14+ foreground-service and MediaProjection requirements are declared correctly.
- [x] A live interpreter session has microphone permission, foreground lifecycle, partial speech recognition, on-device translation, and translated caption overlay.
- [ ] Add speaker separation, translated audio, transcript export, and verified per-device offline labels to the live-caption mode.
- [ ] Sensitive paths disclose screen, microphone, image, copied-text, and retention behavior.
- [ ] Existing geometry, cache, and frame-diff tests pass, and the Android project builds successfully.
