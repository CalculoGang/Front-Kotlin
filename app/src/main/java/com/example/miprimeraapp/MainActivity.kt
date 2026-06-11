package com.example.miprimeraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// ─── Colors ───────────────────────────────────────────────────────────────────

object KigoColors {
    val BgDark       = Color(0xFF0A0A0A)
    val BgMid        = Color(0xFF161616)
    val Surface      = Color.White
    val KigoRed      = Color(0xFFE8341A)
    val VoiceGreen   = Color(0xFF16A34A)
    val VisitBlue    = Color(0xFF2563EB)
    val IdOrange     = Color(0xFFEA580C)
    val TextPrimary  = Color(0xFF111827)
    val TextSecondary= Color(0xFF6B7280)
    val Border       = Color(0xFFE5E7EB)
    val CardBg       = Color(0xFFF9FAFB)
    val Pending      = Color(0xFF9CA3AF)
    val GreenLight   = Color(0xFFDCFCE7)
    val RedLight     = Color(0xFFFEE2E2)
}

// ─── Navigation ───────────────────────────────────────────────────────────────

enum class AppScreen { WELCOME, MODE_SELECT, TOUCH_FORM, VOICE, SUCCESS }

// ─── Form state ───────────────────────────────────────────────────────────────

data class TouchFormData(
    val nombre: String      = "",
    val empresa: String     = "",
    val telefono: String    = "",
    val motivo: String      = "",
    val contacto: String    = "",
    val tipoVisita: String  = "",
    val tipoId: String      = "",
    val observaciones: String = "",
    val step: Int           = 1
)

// ─── Face UI state (unchanged) ────────────────────────────────────────────────

data class FaceUIState(
    val hayRostro: Boolean       = false,
    val vector: List<Float>      = emptyList(),
    val nombreReconocido: String? = null,
    val distancia: Float         = Float.MAX_VALUE
)

// ─── MainActivity ─────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val personasDB  = ConcurrentHashMap<String, MutableList<List<Float>>>()
    private val faceUIState = mutableStateOf(FaceUIState())
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null
    private var faceEmbedder: FaceEmbedder? = null

    private val currentScreen  = mutableStateOf(AppScreen.WELCOME)
    private val touchFormData  = mutableStateOf(TouchFormData())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FaceStorage.cargar(this).forEach { (nombre, vectores) ->
            personasDB[nombre] = vectores
        }

        try {
            faceEmbedder = FaceEmbedder(this)
        } catch (e: Exception) {
            Log.e("FaceEmbedder", "Error cargando modelo .tflite: ${e.message}")
        }

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        faceDetector = FaceDetection.getClient(options)

        pedirPermisoCamara()

        setContent {
            KigoApp(
                currentScreen  = currentScreen.value,
                onNavigate     = { screen -> currentScreen.value = screen },
                faceUIState    = faceUIState.value,
                touchFormData  = touchFormData.value,
                onFormUpdate   = { touchFormData.value = it },
                onGuardarFace  = ::guardarPersona,
                onSetupCamera  = ::setupCamera
            )
        }
    }

    private fun guardarPersona(nombre: String, vector: List<Float>) {
        if (nombre.isBlank() || vector.isEmpty()) return
        personasDB.getOrPut(nombre) { mutableListOf() }.add(vector)
        FaceStorage.guardar(this, personasDB)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(cameraExecutor) { imageProxy ->
                            procesarFrame(imageProxy)
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                Log.e("Camera", "Error configurando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun procesarFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector?.process(image)
            ?.addOnSuccessListener(cameraExecutor) { rostros ->
                val embedder = faceEmbedder
                if (rostros.isNotEmpty() && embedder != null) {
                    try {
                        val bmp  = ImageUtils.rotate(ImageUtils.toBitmap(imageProxy), imageProxy.imageInfo.rotationDegrees)
                        val face = ImageUtils.cropFace(bmp, rostros[0].boundingBox)
                        if (face != null) {
                            val vector = embedder.embed(face).toList()
                            val (nombre, dist) = FaceRecognitionEngine.reconocer(vector, personasDB)
                            faceUIState.value = FaceUIState(
                                hayRostro        = true,
                                vector           = vector,
                                nombreReconocido = nombre,
                                distancia        = dist
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FaceEmbed", "Error embedding: ${e.message}")
                    }
                } else {
                    faceUIState.value = FaceUIState(hayRostro = false)
                }
            }
            ?.addOnFailureListener { e -> Log.e("FaceDetect", "Error: ${e.message}") }
            ?.addOnCompleteListener { imageProxy.close() }
    }

    private fun pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector?.close()
        faceEmbedder?.close()
    }
}

// ─── Root composable ──────────────────────────────────────────────────────────

@Composable
fun KigoApp(
    currentScreen : AppScreen,
    onNavigate    : (AppScreen) -> Unit,
    faceUIState   : FaceUIState,
    touchFormData : TouchFormData,
    onFormUpdate  : (TouchFormData) -> Unit,
    onGuardarFace : (String, List<Float>) -> Unit,
    onSetupCamera : (PreviewView) -> Unit
) {
    when (currentScreen) {
        AppScreen.WELCOME     -> WelcomeScreen    { onNavigate(AppScreen.MODE_SELECT) }
        AppScreen.MODE_SELECT -> ModeSelectScreen(
            onBack        = { onNavigate(AppScreen.WELCOME) },
            onSelectTouch = { onNavigate(AppScreen.TOUCH_FORM) },
            onSelectVoice = { onNavigate(AppScreen.VOICE) }
        )
        AppScreen.TOUCH_FORM  -> TouchFormScreen(
            formData  = touchFormData,
            onUpdate  = onFormUpdate,
            onBack    = { onNavigate(AppScreen.MODE_SELECT) },
            onSubmit  = { onNavigate(AppScreen.SUCCESS) }
        )
        AppScreen.VOICE       -> VoiceScreen(
            faceUIState    = faceUIState,
            onGuardarFace  = onGuardarFace,
            onSetupCamera  = onSetupCamera,
            onBack         = { onNavigate(AppScreen.MODE_SELECT) },
            onSubmit       = { onNavigate(AppScreen.SUCCESS) }
        )
        AppScreen.SUCCESS     -> SuccessScreen(
            formData  = touchFormData,
            onReset   = {
                onFormUpdate(TouchFormData())
                onNavigate(AppScreen.WELCOME)
            }
        )
    }
}

// ─── SCREEN 1 · Welcome ───────────────────────────────────────────────────────

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    var timeText by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); timeText = currentTime() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(KigoColors.BgDark, KigoColors.BgMid))
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(KigoColors.KigoRed, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text("Kigo", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Headline
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bienvenido al",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Self Check-in",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle  = FontStyle.Italic,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Registro de proveedores rápido, seguro y sin filas.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Live clock pill
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(KigoColors.VoiceGreen, CircleShape)
                )
                Text(
                    text = "$timeText · Sistema activo",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            // CTA buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
                ) {
                    Text("Iniciar Registro", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape  = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text("Tengo cita previa", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
            }

            Text(
                "KIGO SELF CHECK-IN AI · v2.4",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// ─── SCREEN 2 · Mode Selection ────────────────────────────────────────────────

@Composable
fun ModeSelectScreen(
    onBack        : () -> Unit,
    onSelectTouch : () -> Unit,
    onSelectVoice : () -> Unit
) {
    val context = LocalContext.current
    var showAudioDialog by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onSelectVoice()
        // if denied, dialog was already dismissed — user stays on this screen
    }

    fun handleVoiceTap() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onSelectVoice()
        else showAudioDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Back
            BackChip(label = "Volver", onClick = onBack)

            // Headline
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Elige tu modo\nde registro",
                    fontSize    = 28.sp,
                    fontWeight  = FontWeight.Bold,
                    color       = KigoColors.TextPrimary,
                    lineHeight  = 34.sp
                )
                Text(
                    "Selecciona la opción que prefieras.",
                    fontSize = 14.sp,
                    color    = KigoColors.TextSecondary
                )
            }

            // Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModeCard(
                    badge      = "TÁCTIL",
                    badgeColor = KigoColors.KigoRed,
                    title      = "Captura táctil",
                    desc       = "Escribe tu información directamente en pantalla.",
                    accentColor= KigoColors.KigoRed,
                    icon       = "✋",
                    onClick    = onSelectTouch
                )
                ModeCard(
                    badge      = "VOZ + IA",
                    badgeColor = KigoColors.VoiceGreen,
                    title      = "Asistencia por voz",
                    desc       = "Un asistente con IA te guía paso a paso.",
                    accentColor= KigoColors.VoiceGreen,
                    icon       = "🎤",
                    onClick    = ::handleVoiceTap
                )
            }
        }
    }

    // Audio permission rationale dialog
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            icon = {
                Text("🎤", fontSize = 32.sp)
            },
            title = {
                Text(
                    "Permiso de micrófono",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El asistente de voz necesita acceso al micrófono para:",
                        fontSize = 14.sp,
                        color    = KigoColors.TextSecondary
                    )
                    PermissionBullet("Capturar tu voz y transcribirla con IA")
                    PermissionBullet("Guiarte paso a paso en el registro")
                    PermissionBullet("Extraer automáticamente tus datos")
                    Text(
                        "Tu audio no se almacena ni se comparte con terceros.",
                        fontSize   = 12.sp,
                        color      = KigoColors.TextSecondary,
                        fontStyle  = FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAudioDialog = false
                        audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KigoColors.VoiceGreen),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Permitir micrófono", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("Cancelar", color = KigoColors.TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PermissionBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Text("•", color = KigoColors.VoiceGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text, fontSize = 14.sp, color = KigoColors.TextPrimary)
    }
}

@Composable
private fun ModeCard(
    badge      : String,
    badgeColor : Color,
    title      : String,
    desc       : String,
    accentColor: Color,
    icon       : String,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
            .background(KigoColors.CardBg)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(icon, fontSize = 28.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
                Text(desc,  fontSize = 13.sp, color = KigoColors.TextSecondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Comenzar", fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                Text("→", color = accentColor, fontSize = 13.sp)
            }
        }
    }
}

// ─── SCREEN 3 · Touch Form ────────────────────────────────────────────────────

@Composable
fun TouchFormScreen(
    formData : TouchFormData,
    onUpdate : (TouchFormData) -> Unit,
    onBack   : () -> Unit,
    onSubmit : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
            .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackChip(label = "Modo", onClick = onBack)
                Column {
                    Text("Registro de Proveedor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
                    Text("Captura táctil · Kigo Self Check-in", fontSize = 11.sp, color = KigoColors.TextSecondary)
                }
            }
            StepPips(total = 4, current = formData.step, accentColor = KigoColors.KigoRed)
        }

        Spacer(Modifier.height(20.dp))

        // Form card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
                .background(KigoColors.CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (formData.step) {
                    1 -> TouchStep1(formData, onUpdate)
                    2 -> TouchStep2(formData, onUpdate)
                    3 -> TouchStep3(formData, onUpdate)
                    4 -> TouchStep4(formData, onUpdate)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (formData.step > 1) {
                OutlinedButton(
                    onClick = { onUpdate(formData.copy(step = formData.step - 1)) },
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("← Anterior", color = KigoColors.TextPrimary)
                }
            } else Spacer(Modifier.width(1.dp))

            Button(
                onClick = {
                    if (formData.step < 4) onUpdate(formData.copy(step = formData.step + 1))
                    else onSubmit()
                },
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
            ) {
                Text(
                    if (formData.step < 4) "Siguiente →" else "Confirmar ✓",
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TouchStep1(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "👤", label = "Datos personales", color = KigoColors.KigoRed)
    KigoTextField("Nombre completo *", data.nombre, "Ej. Juan Carlos Martínez López") { onUpdate(data.copy(nombre = it)) }
    KigoTextField("Empresa que representa *", data.empresa, "Nombre de tu empresa") { onUpdate(data.copy(empresa = it)) }
    KigoTextField("Teléfono de contacto", data.telefono, "55 1234 5678") { onUpdate(data.copy(telefono = it)) }
}

@Composable
private fun TouchStep2(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "📋", label = "Datos de la visita", color = KigoColors.VisitBlue)
    KigoTextField("Motivo de visita *", data.motivo, "Ej. Entrega de equipo, revisión de contrato...", multiline = true) { onUpdate(data.copy(motivo = it)) }
    KigoTextField("Persona o área a visitar *", data.contacto, "Nombre o área") { onUpdate(data.copy(contacto = it)) }
    KigoDropdown(
        label   = "Tipo de visita",
        options = listOf("Entrega de mercancía", "Servicio técnico", "Reunión de negocios", "Auditoría / revisión", "Otro"),
        selected= data.tipoVisita,
        onSelect= { onUpdate(data.copy(tipoVisita = it)) }
    )
}

@Composable
private fun TouchStep3(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "🪪", label = "Identificación oficial", color = KigoColors.IdOrange)
    KigoDropdown(
        label   = "Tipo de identificación",
        options = listOf("INE / IFE", "Pasaporte", "Licencia de conducir", "Credencial de empleado"),
        selected= data.tipoId,
        onSelect= { onUpdate(data.copy(tipoId = it)) }
    )
    // ID Capture area (visual placeholder)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📷", fontSize = 28.sp)
            Text("Toca para escanear tu ID", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Acerca tu identificación a la cámara", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        // Corner decorations
        IdCamCorner(Alignment.TopStart)
        IdCamCorner(Alignment.TopEnd)
        IdCamCorner(Alignment.BottomStart)
        IdCamCorner(Alignment.BottomEnd)
    }
}

@Composable
private fun TouchStep4(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "✅", label = "Confirma tus datos", color = KigoColors.VoiceGreen)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        ConfirmRow("Nombre",       data.nombre.ifBlank { "--" })
        ConfirmRow("Empresa",      data.empresa.ifBlank { "--" })
        ConfirmRow("Motivo",       data.motivo.ifBlank { "--" })
        ConfirmRow("Visita a",     data.contacto.ifBlank { "--" })
        ConfirmRow("Identificación", data.tipoId.ifBlank { "--" }, last = true)
    }
    KigoTextField("Observaciones adicionales", data.observaciones, "Opcional...", multiline = true) {
        onUpdate(data.copy(observaciones = it))
    }
}

@Composable
private fun ConfirmRow(label: String, value: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KigoColors.TextPrimary)
        }
        if (!last) HorizontalDivider(color = KigoColors.Border, thickness = 0.5.dp)
    }
}

// ─── SCREEN 4 · Voice / Face Recognition ──────────────────────────────────────

@Composable
fun VoiceScreen(
    faceUIState   : FaceUIState,
    onGuardarFace : (String, List<Float>) -> Unit,
    onSetupCamera : (PreviewView) -> Unit,
    onBack        : () -> Unit,
    onSubmit      : () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nombreInput    by remember { mutableStateOf("") }
    var micActive      by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KigoColors.BgDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(KigoColors.VoiceGreen, CircleShape)
                )
                Text("Kigo Kiosk", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepPips(total = 5, current = 1, accentColor = KigoColors.VoiceGreen)
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }

        // Camera + face recognition panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black)
        ) {
            CameraPreview(onSetupCamera = onSetupCamera)

            // Face recognition overlay status
            val (chipColor, chipText) = when {
                !faceUIState.hayRostro          -> Pair(Color.Black.copy(alpha = 0.6f), "Sin rostro")
                faceUIState.nombreReconocido != null -> Pair(KigoColors.VoiceGreen.copy(alpha = 0.85f), faceUIState.nombreReconocido!!)
                else                            -> Pair(KigoColors.KigoRed.copy(alpha = 0.85f), "Desconocido")
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(chipColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(chipText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // CAMARA EN VIVO label
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(6.dp).background(KigoColors.KigoRed, CircleShape))
                    Text("CÁMARA EN VIVO", color = Color.White, fontSize = 9.sp, letterSpacing = 0.5.sp)
                }
            }
            // Camera corners
            IdCamCorner(Alignment.TopStart)
            IdCamCorner(Alignment.TopEnd)
            IdCamCorner(Alignment.BottomStart)
            IdCamCorner(Alignment.BottomEnd)
        }

        // Scrollable content below camera
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // AI data extract panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Datos extraídos por IA",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = KigoColors.TextSecondary,
                    letterSpacing = 0.5.sp
                )
                DataExtractRow(icon = "👤", field = "Nombre completo",  value = "Esperando...")
                DataExtractRow(icon = "🏢", field = "Empresa",          value = "Esperando...")
                DataExtractRow(icon = "📋", field = "Motivo de visita", value = "Esperando...")
                DataExtractRow(icon = "📞", field = "Persona a visitar",value = "Esperando...")
                DataExtractRow(icon = "🪪", field = "Identificación",   value = "Esperando...")
            }

            HorizontalDivider(color = KigoColors.Border)

            // Chat area placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Asistente de Registro", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
                // Initial assistant message
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(KigoColors.VoiceGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", color = KigoColors.VoiceGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(KigoColors.CardBg, RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp))
                            .border(1.dp, KigoColors.Border, RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "¡Hola! Soy Kigo, tu asistente de registro. Presiona el micrófono y dime tu nombre completo para comenzar.",
                            fontSize   = 13.sp,
                            color      = KigoColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Bottom mic controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KigoColors.CardBg)
                .border(1.dp, KigoColors.Border)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Voice state bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (micActive) {
                    // Animated waveform dots
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(width = 3.dp, height = (8 + it * 4).dp)
                                .background(KigoColors.VoiceGreen, RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Escuchando...", fontSize = 13.sp, color = KigoColors.VoiceGreen, fontWeight = FontWeight.Medium)
                } else {
                    Text(
                        "Presiona el micrófono para ",
                        fontSize = 13.sp,
                        color    = KigoColors.TextSecondary
                    )
                    Text("iniciar", fontSize = 13.sp, color = KigoColors.VoiceGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            // Mic button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (micActive) KigoColors.VoiceGreen else KigoColors.KigoRed,
                        CircleShape
                    )
                    .clickable { micActive = !micActive },
                contentAlignment = Alignment.Center
            ) {
                Text("🎤", fontSize = 26.sp)
            }

            Text(
                "Toca para hablar · Kigo AI v2.4",
                fontSize = 11.sp,
                color    = KigoColors.TextSecondary
            )
        }
    }

    // Register face dialog (preserves existing face recognition functionality)
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Registrar persona") },
            text = {
                Column {
                    Text("Ingresa el nombre para esta cara:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value       = nombreInput,
                        onValueChange = { nombreInput = it },
                        label       = { Text("Nombre") },
                        singleLine  = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick  = { onGuardarFace(nombreInput, faceUIState.vector); mostrarDialogo = false },
                    enabled  = nombreInput.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }

    // Show register button when face detected
    if (faceUIState.hayRostro) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Handled via FAB-style chip at bottom of camera — access via long press or dedicated button in real impl
        }
    }
}

@Composable
private fun DataExtractRow(icon: String, field: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, KigoColors.Border, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(field, fontSize = 11.sp, color = KigoColors.TextSecondary)
            Text(value, fontSize = 12.sp, color = KigoColors.Pending)
        }
    }
}

// ─── SCREEN 5 · Success ───────────────────────────────────────────────────────

@Composable
fun SuccessScreen(
    formData : TouchFormData,
    onReset  : () -> Unit
) {
    var timeText by remember { mutableStateOf(currentTime()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Check icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(KigoColors.VoiceGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text("Acceso ", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KigoColors.TextPrimary)
                    Text("Autorizado", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KigoColors.VoiceGreen, fontStyle = FontStyle.Italic)
                }
                Text(
                    "Tu registro fue completado. Tu contacto ha sido notificado.",
                    fontSize  = 14.sp,
                    color     = KigoColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Ticket card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KigoColors.BgDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Kigo Self Check-in AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Box(
                        modifier = Modifier
                            .background(KigoColors.VoiceGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("✓ Autorizado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // QR placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(KigoColors.CardBg, RoundedCornerShape(8.dp))
                                .border(1.dp, KigoColors.Border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("QR", fontSize = 20.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Text("Muestra en entrada", fontSize = 11.sp, color = KigoColors.TextSecondary)
                    }
                }
                // Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    ConfirmRow("Visitante", formData.nombre.ifBlank { "--" })
                    ConfirmRow("Empresa",   formData.empresa.ifBlank { "--" })
                    ConfirmRow("Motivo",    formData.motivo.ifBlank { "--" })
                    ConfirmRow("Visita a",  formData.contacto.ifBlank { "--" })
                    ConfirmRow("Hora de entrada", timeText, last = true)
                }
                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KigoColors.CardBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Código válido por 24 horas · Presenta al personal de seguridad",
                        fontSize  = 11.sp,
                        color     = KigoColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = onReset,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
                ) {
                    Text("↺  Nuevo registro", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = onReset,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Inicio", color = KigoColors.TextPrimary)
                }
            }
        }
    }
}

// ─── Shared helper composables ────────────────────────────────────────────────

@Composable
fun CameraPreview(onSetupCamera: (PreviewView) -> Unit) {
    val context     = LocalContext.current
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(Unit) { onSetupCamera(previewView) }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun BackChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(20.dp))
            .background(KigoColors.CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("←", color = KigoColors.TextSecondary, fontSize = 13.sp)
        Text(label, color = KigoColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun StepPips(total: Int, current: Int, accentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(if (i + 1 == current) 20.dp else 8.dp, 8.dp)
                    .background(
                        if (i + 1 <= current) accentColor else KigoColors.Border,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun FormSectionTitle(icon: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 14.sp)
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
    }
}

@Composable
private fun KigoTextField(
    label     : String,
    value     : String,
    placeholder: String,
    multiline : Boolean = false,
    onValue   : (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value         = value,
            onValueChange = onValue,
            placeholder   = { Text(placeholder, fontSize = 13.sp, color = KigoColors.Pending) },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = if (multiline) 3 else 1,
            maxLines      = if (multiline) 5 else 1,
            singleLine    = !multiline,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = KigoColors.Border,
                focusedBorderColor   = KigoColors.KigoRed
            )
        )
    }
}

@Composable
private fun KigoDropdown(
    label   : String,
    options : List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                selected.ifBlank { "Selecciona..." },
                fontSize = 13.sp,
                color    = if (selected.isBlank()) KigoColors.Pending else KigoColors.TextPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt, fontSize = 13.sp) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.IdCamCorner(alignment: Alignment) {
    val (paddingStart, paddingTop) = when (alignment) {
        Alignment.TopStart    -> Pair(0.dp, 0.dp)
        Alignment.TopEnd      -> Pair(0.dp, 0.dp)
        Alignment.BottomStart -> Pair(0.dp, 0.dp)
        else                  -> Pair(0.dp, 0.dp)
    }
    // Corner decoration using border segments
    val cornerMod = when (alignment) {
        Alignment.TopStart    -> Modifier.align(Alignment.TopStart).padding(8.dp)
        Alignment.TopEnd      -> Modifier.align(Alignment.TopEnd).padding(8.dp)
        Alignment.BottomStart -> Modifier.align(Alignment.BottomStart).padding(8.dp)
        else                  -> Modifier.align(Alignment.BottomEnd).padding(8.dp)
    }
    Box(
        modifier = cornerMod
            .size(18.dp)
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = when (alignment) {
                    Alignment.TopStart    -> RoundedCornerShape(topStart = 4.dp)
                    Alignment.TopEnd      -> RoundedCornerShape(topEnd = 4.dp)
                    Alignment.BottomStart -> RoundedCornerShape(bottomStart = 4.dp)
                    else                  -> RoundedCornerShape(bottomEnd = 4.dp)
                }
            )
    )
}

// ─── Utilities ────────────────────────────────────────────────────────────────

private fun currentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
