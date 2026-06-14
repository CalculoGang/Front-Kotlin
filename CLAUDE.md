# KIGO Front-Kotlin — Claude Context

## Project

Android kiosk app for visitor self-check-in. Residents are identified via frontal camera (TFLite face embeddings) and voice interaction drives the conversation flow. Kotlin + Jetpack Compose only — no XML layouts.

## Tech stack

| Layer | Tech |
|---|---|
| UI | Jetpack Compose + Material3 |
| Camera | CameraX 1.4.2 |
| Face detection | ML Kit `face-detection:16.1.7` |
| Face recognition | TFLite `mobile_face_net.tflite` (128-dim cosine) |
| Speech-to-Text | Android native `SpeechRecognizer` |
| Persistence | `faces.json` in `filesDir` via `FaceStorage.kt` |

## Key files

```
MainActivity.kt              # camera lifecycle, permissions, face pipeline
KigoApp.kt                   # navigation root (when/AppScreen enum)
model/Models.kt              # AppScreen, TouchFormData, FaceUIState
ui/theme/Color.kt            # KigoColors object — single source of truth for colors
ui/components/Components.kt  # shared composables (BackChip, KigoTextField, etc.)
ui/components/ChatComponents.kt  # ChatMessage sealed class + all chat bubbles
ui/screens/VoiceScreen.kt    # main screen: camera + SpeechRecognizer + live chat
```

## Patterns

- **State flows down, events flow up** — all screens receive state + lambda callbacks, never ViewModel directly (MainActivity holds state).
- **ChatMessage sealed class** lives in `ChatComponents.kt`. Add new message types there; `ConversationList` handles rendering via `when`.
- **SpeechRecognizer** is created with `remember`, wired in `DisposableEffect(Unit)`, destroyed in `onDispose`. Never recreate mid-session.
- Colors: always use `KigoColors.*` — never hardcode hex in screen files unless it's a one-off shadow alpha.
- No mock data in production screens. VoiceScreen messages start empty (one greeting from Kigo).

## Active branch

`feature/android-speech-recognizer-test` — integrates live STT into VoiceScreen.

## What not to do

- Don't add ViewModel or Hilt — architecture is intentionally simple (single Activity state).
- Don't use `rememberCoroutineScope` outside composables.
- Don't call `SpeechRecognizer` from a background thread — it must run on the main thread.
- Don't add XML layouts or fragments.
