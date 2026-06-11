package com.example.miprimeraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object AppColors {
    val BLANCO = Color.White
    val NARANJA = Color(0xFFFF9800)
    val VERDE = Color(0xFF4CAF50)
    val ROJO = Color(0xFFF44336)
    val GRIS = Color(0xFF9E9E9E)
    val FONDO_VECTOR = Color(0xFFF5F5F5)
    val TEXTO_OSCURO = Color(0xFF333333)
}

data class FaceUIState(
    val hayRostro: Boolean = false,
    val vector: List<Float> = emptyList(),
    val nombreReconocido: String? = null,
    val distancia: Float = Float.MAX_VALUE
)

class MainActivity : ComponentActivity() {

    private val personasDB = ConcurrentHashMap<String, MutableList<List<Float>>>()
    private val faceUIState = mutableStateOf(FaceUIState())
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null
    private var faceEmbedder: FaceEmbedder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FaceStorage.cargar(this).forEach { (nombre, vectores) ->
            personasDB[nombre] = vectores
        }

        try {
            faceEmbedder = FaceEmbedder(this)
        } catch (e: Exception) {
            Log.e("FaceEmbedder", "No se pudo cargar el modelo .tflite: ${e.message}")
        }

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        faceDetector = FaceDetection.getClient(options)

        pedirPermisoCamara()

        setContent {
            PantallaReconocimientoFacial(
                uiState = faceUIState.value,
                onGuardar = ::guardarPersona,
                onSetupCamera = ::setupCamera
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
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("Camera", "Error configurando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun procesarFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector?.process(image)
            ?.addOnSuccessListener(cameraExecutor) { rostros ->
                val embedder = faceEmbedder
                if (rostros.isNotEmpty() && embedder != null) {
                    try {
                        val bmp = ImageUtils.rotate(
                            ImageUtils.toBitmap(imageProxy),
                            imageProxy.imageInfo.rotationDegrees
                        )
                        val face = ImageUtils.cropFace(bmp, rostros[0].boundingBox)
                        if (face != null) {
                            val vector = embedder.embed(face).toList()
                            val (nombre, dist) = FaceRecognitionEngine.reconocer(vector, personasDB)
                            faceUIState.value = FaceUIState(
                                hayRostro = true,
                                vector = vector,
                                nombreReconocido = nombre,
                                distancia = dist
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FaceEmbed", "Error generando embedding: ${e.message}")
                    }
                } else {
                    faceUIState.value = FaceUIState(hayRostro = false)
                }
            }
            ?.addOnFailureListener { e ->
                Log.e("FaceDetect", "Error: ${e.message}")
            }
            ?.addOnCompleteListener {
                imageProxy.close()
            }
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

@Composable
fun PantallaReconocimientoFacial(
    uiState: FaceUIState,
    onGuardar: (String, List<Float>) -> Unit,
    onSetupCamera: (PreviewView) -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nombreInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BLANCO)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reconocimiento Facial",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TEXTO_OSCURO
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CameraPreview(onSetupCamera = onSetupCamera)
        }

        val (statusColor, statusText) = when {
            !uiState.hayRostro -> Pair(AppColors.GRIS, "Sin rostro detectado")
            uiState.nombreReconocido != null -> Pair(
                AppColors.VERDE,
                "Hola, ${uiState.nombreReconocido}!  (dist: ${"%.3f".format(uiState.distancia)})"
            )
            else -> Pair(
                AppColors.ROJO,
                "Persona desconocida  (dist: ${"%.3f".format(uiState.distancia)})"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (uiState.vector.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.FONDO_VECTOR, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Embedding facial (${uiState.vector.size} valores):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AppColors.TEXTO_OSCURO
                    )
                    Spacer(Modifier.height(6.dp))
                    val vectorStr = uiState.vector
                        .chunked(4)
                        .joinToString("\n") { row ->
                            row.joinToString("  ") { "%.3f".format(it) }
                        }
                    Text(
                        text = vectorStr,
                        fontSize = 11.sp,
                        color = Color(0xFF555555),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (uiState.hayRostro) {
            Button(
                onClick = {
                    nombreInput = uiState.nombreReconocido ?: ""
                    mostrarDialogo = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NARANJA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.nombreReconocido != null) "Actualizar registro" else "Registrar esta cara",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Registrar persona") },
            text = {
                Column {
                    Text("Ingresa el nombre para esta cara:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nombreInput,
                        onValueChange = { nombreInput = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGuardar(nombreInput, uiState.vector)
                        mostrarDialogo = false
                    },
                    enabled = nombreInput.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun CameraPreview(onSetupCamera: (PreviewView) -> Unit) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        onSetupCamera(previewView)
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
