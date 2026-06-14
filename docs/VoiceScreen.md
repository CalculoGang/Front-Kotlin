# VoiceScreen

Main interaction screen. Combines frontal camera (face recognition) with live voice-to-text conversation.

---

## Layout overview

```
┌─────────────────────────────────┐
│  KigoAppleHeader  (KIGO / LAS CUMBRES)    │
├──────────┬──────────────────────┤
│ Camera   │                      │  ← weight(1f) zone
│ Portrait │    (empty space)     │
│ Block    │            🐶 mascot  │  ← mascot floats at top-end, offset y -185dp
├──────────┴──────────────────────┤
│       GlassChatCard  330dp      │
│  ┌──────────────────────────┐   │
│  │  ConversationList        │   │
│  │  · KigoBubble (gray)     │   │
│  │  · UserBubble (red)      │   │
│  │  · UserPartialBubble     │   │
│  │  · TypingBubble          │   │
│  └──────────────────────────┘   │
│  [ quick action chips row ]     │
├─────────────────────────────────┤
│       AppleBottomNav            │
│  🏠   🎤 (mic / waveform)   ⚙   │
└─────────────────────────────────┘
```

---

## State

| Variable | Type | Purpose |
|---|---|---|
| `micActive` | `Boolean` | true = recognizer running → shows WaveformBars in mic button |
| `mostrarDialogo` | `Boolean` | shows RegisterFaceDialog |
| `nombreInput` | `String` | name input for face registration |
| `hasPermission` | `Boolean` | RECORD_AUDIO grant status |
| `messages` | `SnapshotStateList<ChatMessage>` | live conversation list |

---

## SpeechRecognizer integration

The recognizer is created once via `remember` and wired inside `DisposableEffect(Unit)`. It is destroyed in `onDispose` to avoid leaks.

```kotlin
val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

DisposableEffect(Unit) {
    recognizer.setRecognitionListener(...)
    onDispose {
        recognizer.stopListening()
        recognizer.destroy()
    }
}
```

### Recognition lifecycle → chat events

```
startListening()
    │
    ├─ onReadyForSpeech  → micActive = true
    │
    ├─ onPartialResults  → removeAll UserPartial
    │                       add UserPartial(partial[0])       ← live bubble updates
    │
    ├─ onEndOfSpeech     → (no-op, results arrive next)
    │
    ├─ onResults         → removeAll UserPartial
    │                       add User(final)
    │                       add Typing
    │                       delay(1500ms)
    │                       remove Typing
    │                       add Kigo("(audio recibido)")      ← bot simulated reply
    │
    └─ onError           → micActive = false
                           removeAll UserPartial
```

### Intent configuration

```kotlin
Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
    putExtra(EXTRA_LANGUAGE,       Locale.getDefault())
    putExtra(EXTRA_PARTIAL_RESULTS, true)   // enables live transcription
    putExtra(EXTRA_MAX_RESULTS,    3)
}
```

---

## ChatMessage types

Defined in `ui/components/ChatComponents.kt`.

| Type | Alignment | Visual | When added |
|---|---|---|---|
| `Kigo(text)` | Left | Gray bubble `#EEEEF4` | Bot response / greeting |
| `User(text)` | Right | Solid red bubble | Final speech result |
| `UserPartial(text)` | Right | Faded italic red bubble | Live partial transcription |
| `ResidentFound(name, address)` | Left | Card with avatar + active badge | (future) resident match |
| `Typing` | Left | Animated 3-dot bubble | While bot "thinks" |

---

## Private composables

| Composable | File location | Purpose |
|---|---|---|
| `KigoAppleHeader` | VoiceScreen.kt | KIGO / LAS CUMBRES centered header |
| `PortraitCameraBlock` | VoiceScreen.kt | Camera preview + face chip + LIVE dot |
| `MascotImage` | VoiceScreen.kt | Static dog mascot image (no animation) |
| `GlassChatCard` | VoiceScreen.kt | Rounded white card wrapping chat + chips |
| `AppleBottomNav` | VoiceScreen.kt | Home / Mic / Settings pill nav |
| `WaveformBars` | VoiceScreen.kt | Animated bars shown inside mic button when active |
| `RegisterFaceDialog` | VoiceScreen.kt | AlertDialog to name and save a face vector |
| `ConversationList` | ChatComponents.kt | LazyColumn rendering `List<ChatMessage>` |
| `KigoBubble` | ChatComponents.kt | Left-aligned gray bubble |
| `UserBubble` | ChatComponents.kt | Right-aligned solid red bubble |
| `UserPartialBubble` | ChatComponents.kt | Right-aligned faded italic red (live transcription) |
| `TypingBubble` | ChatComponents.kt | Animated dots + "Esperando confirmacion..." |
| `QuickActionChip` | ChatComponents.kt | Horizontal scrollable action pills |

---

## Permissions

`RECORD_AUDIO` is checked at startup (via `ContextCompat.checkSelfPermission`). If missing, `startListening()` triggers `permLauncher` instead of starting the recognizer. The user only sees a system permission dialog — no custom screen.

---

## Extending the bot response

Currently the bot always replies `"(audio recibido)"`. To wire a real backend:

1. Replace the `delay + Kigo("(audio recibido)")` block in `onResults` with an API call.
2. Keep `ChatMessage.Typing` visible while the call is in-flight.
3. On response, remove `Typing` and add `Kigo(responseText)`.

The conversation list handles it automatically — no other changes needed.
