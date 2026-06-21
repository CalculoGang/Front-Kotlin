# KIGO Self Check-in — Android (Kotlin)

App de kiosco para registro de proveedores con captura táctil, asistencia por voz e identificación facial en tiempo real.

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 11 |
| Android SDK | API 24 (Android 7.0) |
| Dispositivo | Con cámara frontal |

> El emulador de Android Studio funciona, pero el reconocimiento facial es más preciso en dispositivo físico.

---

## Cómo correr el proyecto

```bash
git clone https://github.com/CalculoGang/Front-Kotlin.git
cd Front-Kotlin
```

1. Abrir en Android Studio → **File → Open** → seleccionar esta carpeta
2. Esperar sincronización de Gradle (primera vez puede tardar)
3. Conectar dispositivo o iniciar emulador
4. **Run ▶** (Shift+F10)
5. Conceder permisos de **Cámara** y **Micrófono** cuando la app los solicite

### Conectar al backend local (IMPORTANTE)

`baseUrl` por defecto es `http://127.0.0.1:3000/api/v1` (ver `KigoApi.kt`). En un
teléfono físico, `127.0.0.1` es el propio teléfono, no tu PC. Según cómo corras:

- **Teléfono por cable (USB):** redirigir el puerto del PC al teléfono con `adb reverse`:
  ```bash
  adb reverse tcp:3000 tcp:3000
  adb reverse --list   # verificar
  ```
  Mantener `baseUrl = http://127.0.0.1:3000/api/v1`.
  Se borra al desconectar el cable o reiniciar adb → re-ejecutar si falla la conexión.
- **Emulador:** `baseUrl = http://10.0.2.2:3000/api/v1`.
- **Misma WiFi que el PC:** `baseUrl = http://<IP-del-PC>:3000/api/v1` (ej. `192.168.x.x`).

Síntoma de que falta el reverse: `error to connect 127.0.0.1:3000` / `Failed to connect`.

---

## Flujo de la app

```
Welcome → Mode Select → Touch Form (4 pasos) → Success
                     ↘ Voice (cámara + mic)  ↗
```

| Pantalla | Descripción |
|---|---|
| **Welcome** | Pantalla de bienvenida con reloj en vivo |
| **Mode Select** | El usuario elige Táctil o Voz. Voz solicita permiso de micrófono aquí |
| **Touch Form** | Formulario de 4 pasos: datos personales → visita → ID → confirmación |
| **Voice** | Cámara frontal + reconocimiento facial + conversación por voz en tiempo real (SpeechRecognizer) |
| **Admin** | (desde Welcome ⚙) Alta de personas/empresas contra el backend + captura biométrica multi-muestra |
| **Success** | Ticket de acceso con código QR (placeholder) |

---

## Arquitectura (MVVM)

La app sigue **MVVM** con un único ViewModel y una capa de datos. La Activity es un cascarón:
recoge el estado del ViewModel y lo pasa a Compose; no contiene lógica de negocio. DI es una
`ViewModelProvider.Factory` manual — **sin Hilt** (un solo ViewModel no lo necesita).

```
        ┌─────────────┐  state (StateFlow)   ┌──────────────────┐
        │  Compose UI │ ◀─────────────────── │   KigoViewModel  │
        │  (screens)  │ ───────────────────▶ │  uiState/faceState│
        └─────────────┘  eventos (lambdas)   └────────┬─────────┘
                                                       │ suspend
        ┌─────────────┐  (hayRostro, vector) ┌─────────▼─────────┐
        │ FacePipeline│ ───────────────────▶ │   KigoRepository  │
        │ cámara+ML+emb│                      │ KigoApi + cache   │
        └─────────────┘                      └────────┬──────────┘
                                              ┌────────▼──────────┐
                                              │ Backend Go / JSON │
                                              └───────────────────┘
```

- **`KigoViewModel`** es dueño del estado en dos `StateFlow`: `uiState` (pantalla, formulario,
  listas de personas/empresas) y `faceState` (se actualiza por-frame, va aparte para no
  recomponer las listas en cada frame). Toda la lógica + red corre en `viewModelScope`.
- **`KigoRepository`** aísla los datos: funciones `suspend` en `Dispatchers.IO` que envuelven
  `KigoApi` (red) y `AdminStorage` (cache local). El ViewModel nunca toca disco ni red directamente.
- **`FacePipeline`** hace cámara + ML Kit + embedding y emite `(hayRostro, vector)` al ViewModel,
  que decide el lookup biométrico contra el backend (con throttle).

## Estructura del proyecto

```
app/src/main/java/com/example/miprimeraapp/
│
├── MainActivity.kt              # Cascarón: cablea KigoViewModel + FacePipeline + Compose
├── KigoApp.kt                   # Navegación root — decide qué pantalla mostrar
│
├── vm/
│   └── KigoViewModel.kt         # ViewModel único: uiState + faceState (StateFlow), acciones
│
├── data/
│   └── KigoRepository.kt        # Capa de datos: KigoApi + AdminStorage, suspend/Dispatchers.IO
│
├── face/
│   └── FacePipeline.kt          # Cámara + ML Kit + embedding; emite (hayRostro, vector) al VM
│
├── model/
│   └── Models.kt                # AppScreen (enum), TouchFormData, FaceUIState, Persona, Empresa
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # KigoColors — paleta de la app
│   │   ├── Theme.kt             # MaterialTheme (scaffold de Android Studio)
│   │   └── Type.kt              # Tipografía base
│   │
│   ├── components/
│   │   ├── Components.kt        # Composables reutilizables entre pantallas
│   │   │                        # (BackChip, StepPips, KigoTextField, KigoDropdown,
│   │   │                        #  ConfirmRow, CamCorner, DataExtractRow, ModeCard…)
│   │   └── ChatComponents.kt    # ChatMessage sealed class + todos los bubbles del chat
│   │                            # (KigoBubble, UserBubble, UserPartialBubble, TypingBubble,
│   │                            #  ResidentCardBubble, QuickActionChip, ConversationList)
│   │
│   └── screens/
│       ├── WelcomeScreen.kt
│       ├── ModeSelectScreen.kt  # Incluye AudioPermissionDialog
│       ├── TouchFormScreen.kt   # Steps: Step1Personal, Step2Visit, Step3Id, Step4Confirm
│       ├── VoiceScreen.kt       # Cámara + SpeechRecognizer + chat en vivo (ver docs/)
│       ├── SpeechTestScreen.kt  # Pantalla de prueba STT independiente
│       ├── AdminScreen.kt       # Alta de personas/empresas + captura biométrica multi-muestra
│       └── SuccessScreen.kt     # TicketCard con QR placeholder
│
├── KigoApi.kt                   # Cliente HTTP del backend Go (org.json, bloqueante)
├── AdminStorage.kt              # Cache local en admin.json (filesDir)
├── FaceEmbedder.kt              # Carga modelo .tflite, genera embedding (L2-normalizado)
├── FaceRecognitionEngine.kt     # Distancia coseno, promedio de embeddings
└── ImageUtils.kt                # toBitmap (YUV→RGB), rotate, alignFace
```

---

## Cómo agregar una feature

### Nueva pantalla

1. Agregar valor al enum en `model/Models.kt`:
   ```kotlin
   enum class AppScreen { WELCOME, MODE_SELECT, TOUCH_FORM, VOICE, SUCCESS, MI_NUEVA_PANTALLA }
   ```

2. Crear `ui/screens/MiNuevaPantalla.kt`:
   ```kotlin
   package com.example.miprimeraapp.ui.screens

   @Composable
   fun MiNuevaPantalla(onBack: () -> Unit) { ... }
   ```

3. Agregar el `when` branch en `KigoApp.kt`:
   ```kotlin
   AppScreen.MI_NUEVA_PANTALLA -> MiNuevaPantalla(
       onBack = { onNavigate(AppScreen.WELCOME) }
   )
   ```

4. Navegar a ella desde donde corresponda con `onNavigate(AppScreen.MI_NUEVA_PANTALLA)`.

---

### Nuevo componente reutilizable

Agregar en `ui/components/Components.kt`. Si el componente es muy grande o exclusivo de una sola pantalla puede vivir como `private fun` dentro del archivo de esa pantalla.

---

### Nuevo campo en el formulario táctil

1. Agregar el campo en `TouchFormData` (`model/Models.kt`):
   ```kotlin
   data class TouchFormData(
       ...
       val miNuevoDato: String = ""
   )
   ```

2. En el step correspondiente de `TouchFormScreen.kt`, agregar el input:
   ```kotlin
   KigoTextField("Mi campo", data.miNuevoDato, "Placeholder") {
       onUpdate(data.copy(miNuevoDato = it))
   }
   ```

---

### Nuevo color de marca

Agregar en `ui/theme/Color.kt` dentro del objeto `KigoColors`:
```kotlin
object KigoColors {
    ...
    val MiColor = Color(0xFF123456)
}
```

---

## Reconocimiento facial

El pipeline vive en `face/FacePipeline.kt` (cámara + detección + embedding). Emite
`(hayRostro, vector)` al `KigoViewModel`, que consulta el backend (`POST /personas/buscar-rostro`)
con throttle y publica el resultado en `faceState`. La UI (`VoiceScreen`, `AdminScreen`) solo
lee `FaceUIState` por composición — sin dependencia directa entre UI y motor.

| Archivo | Responsabilidad |
|---|---|
| `face/FacePipeline.kt` | Cámara CameraX + ML Kit (cara más grande) + gates de calidad + promedio de N frames |
| `FaceEmbedder.kt` | Carga `mobilefacenet.tflite`, devuelve embedding L2-normalizado (dim leída del modelo) |
| `FaceRecognitionEngine.kt` | Distancia coseno + `promediar()` de embeddings |
| `ImageUtils.kt` | Preprocesado: `toBitmap` (YUV→RGB), `rotate`, `alignFace` (nivela por roll + margen) |

El **match** se hace en el backend (vector de 128 dims). El front solo extrae y promedia el
embedding. Para ajustar qué caras se embeben (calidad), editar las constantes en
`FacePipeline.kt`:
```kotlin
private const val FACE_MIN_PX      = 100   // lado mínimo del box; súbelo si el kiosko está lejos
private const val FACE_MAX_YAW_DEG = 18f   // perfil máximo permitido
private const val FACE_AVG_FRAMES  = 5     // frames buenos a promediar por emisión
```
El intervalo entre lookups al backend está en `KigoViewModel`: `FACE_QUERY_MS = 1200L`.

---

## Cómo agregar lógica de negocio (MVVM)

Estado y red **no** van en la Activity ni en las pantallas — van en el ViewModel y el repositorio.

1. **Dato nuevo de red** → añadir una función `suspend` en `data/KigoRepository.kt` que envuelva
   la llamada de `KigoApi` dentro de `withContext(Dispatchers.IO)`.
2. **Acción/estado nuevo** → en `vm/KigoViewModel.kt`: añadir el campo al `KigoUiState`
   (o al `FaceUIState`) y una función que lance `viewModelScope.launch { ... }` llamando al repo
   y actualizando el `StateFlow` con `_uiState.update { ... }`.
3. **Exponerlo a la UI** → pasar el valor/estado y la lambda desde `MainActivity` →
   `KigoApp` → la pantalla. Las pantallas siguen recibiendo estado + callbacks (nunca el VM directo).
4. **Mensajes al usuario** → emitir por el `SharedFlow` de toasts del VM (`toast("...")`);
   la Activity los muestra. El VM no toca UI.

---

## VoiceScreen — conversación por voz

`VoiceScreen` usa `SpeechRecognizer` nativo de Android para convertir voz a texto en tiempo real. Ver documentación completa en [`docs/VoiceScreen.md`](docs/VoiceScreen.md).

Flujo resumido:
1. Usuario toca botón mic → `startListening()`
2. Mientras habla → resultados parciales aparecen como burbuja roja semitransparente (feedback en vivo)
3. Al terminar → burbuja se confirma, Kigo responde `"(audio recibido)"` tras 1.5 s
4. Volver a tocar mic → detiene escucha

---

## Permisos

| Permiso | Cuándo se pide |
|---|---|
| `CAMERA` | Al arrancar la app (`MainActivity.onCreate`) |
| `RECORD_AUDIO` | Al seleccionar modo Voz (`ModeSelectScreen`) y al tocar mic en `VoiceScreen` |

---

## Dependencias principales

```kotlin
// Jetpack Compose
"androidx.activity:activity-compose"
"androidx.compose.material3:material3"

// MVVM: ViewModel + coroutines + colección lifecycle-aware en Compose
"androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7"
"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
"androidx.lifecycle:lifecycle-runtime-compose:2.8.7"
"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"

// CameraX
"androidx.camera:camera-core:1.4.2"
"androidx.camera:camera-lifecycle:1.4.2"
"androidx.camera:camera-view:1.4.2"

// ML Kit
"com.google.mlkit:face-detection:16.1.7"

// TFLite
"org.tensorflow:tensorflow-lite"
"org.tensorflow:tensorflow-lite-support:0.4.4"
```

---

## Problemas frecuentes

**`Inconsistent JVM-target compatibility`**
→ Verificar en `app/build.gradle.kts`:
```kotlin
kotlinOptions { jvmTarget = "11" }
```

**App queda en "Sin rostro detectado"**
→ Permiso de cámara denegado. Ajustes del sistema → Apps → Permisos → Cámara.

**Gradle no sincroniza**
→ File → Invalidate Caches → Invalidate and Restart

**`error to connect 127.0.0.1:3000` / la app no llega al backend**
→ Teléfono por cable: falta el reverse. Ejecutar `adb reverse tcp:3000 tcp:3000`.
  Ver [Conectar al backend local](#conectar-al-backend-local-importante).
