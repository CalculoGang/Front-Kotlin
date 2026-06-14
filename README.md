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
| **Success** | Ticket de acceso con código QR (placeholder) |

---

## Estructura del proyecto

```
app/src/main/java/com/example/miprimeraapp/
│
├── MainActivity.kt              # Activity: lifecycle, cámara, permisos, face recognition
├── KigoApp.kt                   # Navegación root — decide qué pantalla mostrar
│
├── model/
│   └── Models.kt                # AppScreen (enum), TouchFormData, FaceUIState
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
│       └── SuccessScreen.kt     # TicketCard con QR placeholder
│
├── FaceEmbedder.kt              # Carga modelo .tflite, genera embedding 128-dim
├── FaceRecognitionEngine.kt     # Distancia coseno, umbral de reconocimiento
├── FaceStorage.kt               # Persistencia de vectores en faces.json (filesDir)
└── ImageUtils.kt                # Rotate, toBitmap, cropFace
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

La lógica vive en `MainActivity.kt` (orquestación) y los archivos `Face*.kt` (algoritmos). El resultado se expone como `FaceUIState` y se pasa a `VoiceScreen` vía composición — sin dependencia directa entre la UI y el motor.

| Archivo | Responsabilidad |
|---|---|
| `FaceEmbedder.kt` | Carga `mobile_face_net.tflite`, devuelve vector `float[128]` |
| `FaceRecognitionEngine.kt` | Compara vectores con distancia coseno. Umbral: `THRESHOLD = 0.20f` |
| `FaceStorage.kt` | Lee/escribe `faces.json` en `filesDir` (sin permisos extra) |
| `ImageUtils.kt` | Preprocesado: rotar bitmap, recortar cara del bounding box |

Para ajustar la sensibilidad del reconocimiento editar `FaceRecognitionEngine.kt`:
```kotlin
private const val THRESHOLD = 0.20f  // menor = más estricto
```

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
