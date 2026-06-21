# KIGO Front-Kotlin — Claude Context

## Project

Android kiosk app for visitor self-check-in. Residents are identified via frontal camera (TFLite face embeddings) and voice interaction drives the conversation flow. Kotlin + Jetpack Compose only — no XML layouts.

## Tech stack

| Layer | Tech |
|---|---|
| UI | Jetpack Compose + Material3 |
| Camera | CameraX 1.4.2 |
| Face detection | ML Kit `face-detection:16.1.7` |
| Face recognition | TFLite `mobilefacenet.tflite` (192-dim cosine, dim read from model output tensor) |
| Speech-to-Text | Android native `SpeechRecognizer` |
| Architecture | MVVM — `KigoViewModel` (StateFlow) + `KigoRepository`, coroutines (no Hilt) |
| Persistence | backend (Go) via `KigoApi.kt`; local cache `admin.json` in `filesDir` via `AdminStorage.kt` |

## Key files

```
MainActivity.kt              # thin shell: wires KigoViewModel + FacePipeline + Compose
vm/KigoViewModel.kt          # single ViewModel: uiState + faceState StateFlows, all actions
data/KigoRepository.kt       # data layer: wraps KigoApi + AdminStorage, suspend on Dispatchers.IO
face/FacePipeline.kt         # camera + ML Kit + embedding; emits (hayRostro, vector) to VM
KigoApi.kt                   # Go backend HTTP client (org.json, blocking)
KigoApp.kt                   # navigation root (when/AppScreen enum)
model/Models.kt              # AppScreen, TouchFormData, FaceUIState, Persona, Empresa
ui/theme/Color.kt            # KigoColors object — single source of truth for colors
ui/components/Components.kt  # shared composables (BackChip, KigoTextField, etc.)
ui/components/ChatComponents.kt  # ChatMessage sealed class + all chat bubbles
ui/screens/VoiceScreen.kt    # main screen: camera + SpeechRecognizer + live chat
```

## Patterns

- **MVVM** — `KigoViewModel` owns app state in two `StateFlow`s (`uiState` for screen/form/lists, `faceState` per-frame). Business logic + network run in `viewModelScope` via `KigoRepository`. DI is a manual `ViewModelProvider.Factory` — no Hilt.
- **State flows down, events flow up** — screens still receive plain state + lambda callbacks (the Activity collects VM state and passes it down); screens never touch the ViewModel directly.
- **ChatMessage sealed class** lives in `ChatComponents.kt`. Add new message types there; `ConversationList` handles rendering via `when`.
- **SpeechRecognizer** is created with `remember`, wired in `DisposableEffect(Unit)`, destroyed in `onDispose`. Never recreate mid-session.
- Colors: always use `KigoColors.*` — never hardcode hex in screen files unless it's a one-off shadow alpha.
- No mock data in production screens. VoiceScreen messages start empty (one greeting from Kigo).

## Active branch

`feature/android-speech-recognizer-test` — integrates live STT into VoiceScreen.

## What not to do

- Don't add Hilt — DI is a manual factory; one ViewModel doesn't need a DI framework.
- Don't put state or network in MainActivity/screens — it belongs in `KigoViewModel`/`KigoRepository`.
- Don't use `rememberCoroutineScope` outside composables.
- Don't call `SpeechRecognizer` from a background thread — it must run on the main thread.
- Don't add XML layouts or fragments.
