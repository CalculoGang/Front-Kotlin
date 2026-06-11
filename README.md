# KIGO – Reconocimiento Facial Android

App Android que detecta rostros en tiempo real con CameraX + ML Kit, extrae un vector de landmarks faciales, y recuerda personas usando almacenamiento interno.

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 11 |
| Android SDK | API 24 (Android 7.0) o superior |
| Dispositivo/Emulador | Con cámara frontal |

> **Emulador:** la cámara virtual del emulador de Android Studio funciona, pero el reconocimiento facial es más preciso en dispositivo físico.

## Cómo correr el proyecto

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/<tu-usuario>/KIGO-PROYECT.git
   cd KIGO-PROYECT/Front-Kotlin
   ```

2. **Abrir en Android Studio**
   - File → Open → seleccionar la carpeta `Front-Kotlin`
   - Esperar a que Gradle sincronice (puede tardar unos minutos la primera vez)

3. **Sincronizar Gradle**
   - Si Android Studio pide sincronizar, hacer clic en **Sync Now**
   - Verificar que no haya errores en el panel Build

4. **Ejecutar**
   - Conectar dispositivo físico con **USB Debugging** activado, o iniciar un emulador
   - Presionar el botón **Run ▶** (Shift+F10)
   - Conceder permiso de **cámara** cuando la app lo solicite

## Funcionalidades

### Detección de rostros
- Usa **ML Kit Face Detection** para detectar rostros en tiempo real
- Identifica 10 landmarks: ojos, nariz, boca, orejas, mejillas

### Vector facial
- Extrae 20 valores flotantes (coordenadas X,Y de cada landmark normalizadas al bounding box del rostro)
- El vector se muestra en pantalla en tiempo real para visualizar cómo se "ve" la cara para el sistema

### Reconocimiento
- Compara el vector actual contra los vectores guardados usando **distancia euclidiana**
- Umbral de reconocimiento: `0.45` (ajustable en `FaceRecognitionEngine.kt`)
- Verde = persona conocida · Rojo = desconocida · Gris = sin rostro

### Memoria persistente
- Los vectores faciales se guardan en `filesDir/faces.json` (almacenamiento interno, sin permisos extra)
- Para **registrar una cara**: detectar rostro → botón "Registrar esta cara" → escribir nombre → Guardar
- Los registros persisten entre sesiones

## Estructura del proyecto

```
app/src/main/java/com/example/miprimeraapp/
├── MainActivity.kt          # UI (Compose), cámara, flujo principal
├── FaceRecognitionEngine.kt # Extracción de vector + comparación
└── FaceStorage.kt           # Guardado/carga de vectores en JSON
```

## Dependencias principales

```kotlin
// CameraX
"androidx.camera:camera-core:1.4.2"
"androidx.camera:camera-camera2:1.4.2"
"androidx.camera:camera-lifecycle:1.4.2"
"androidx.camera:camera-view:1.4.2"

// ML Kit
"com.google.mlkit:face-detection:16.1.7"
```

## Problemas frecuentes

**`Inconsistent JVM-target compatibility`**
→ Asegurarse de que `app/build.gradle.kts` tenga:
```kotlin
kotlinOptions {
    jvmTarget = "11"
}
```

**La app se queda en "Sin rostro detectado"**
→ Verificar que se concedió permiso de cámara. Si fue denegado: Configuración del sistema → Apps → permisos → Cámara.

**Gradle no sincroniza**
→ File → Invalidate Caches → Invalidate and Restart
