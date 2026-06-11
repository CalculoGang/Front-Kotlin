package com.example.miprimeraapp

import android.Manifest
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ============================= COLORES DE LA APP =============================
object AppColors {
    val BLANCO = Color.White
    val NARANJA = Color(0xFFFF9800)
    val TEXTO_OSCURO = Color(0xFF333333)
}

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var faceDetector: FaceDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraExecutor = Executors.newSingleThreadExecutor()
        inicializarDetectorRostros()
        pedirPermisoCamara()

        setContent {
            PantallaReconocimientoFacial()
        }
    }

    private fun inicializarDetectorRostros() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    private fun pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISO_CAMARA_REQUEST_CODE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector?.close()
    }

    companion object {
        private const val PERMISO_CAMARA_REQUEST_CODE = 10
    }
}

@Composable
fun PantallaReconocimientoFacial() {
    var coordenadasRostro by remember { mutableStateOf("Detectando rostro...") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BLANCO),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Reconocimiento Facial",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TEXTO_OSCURO
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.Black)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview()
            }

            Box(
                modifier = Modifier
                    .background(AppColors.NARANJA, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = coordenadasRostro,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        configurarCamara(context, previewView)
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .size(300.dp)
            .background(Color.Black)
    )
}

private fun configurarCamara(context: android.content.Context, previewView: PreviewView) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener(
        {
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            Executors.newSingleThreadExecutor()
                        ) { imageProxy ->
                            procesarFrameMLKit(imageProxy)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Log.e("CameraPreview", "Error configurando cámara: ${e.message}")
            }
        },
        ContextCompat.getMainExecutor(context)
    )
}

@androidx.camera.core.ExperimentalGetImage
@Suppress("UNCHECKED_CAST")
private fun procesarFrameMLKit(imageProxy: ImageProxy) {
    try {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val detector = FaceDetection.getClient()

            detector.process(image)
                .addOnSuccessListener { rostros ->
                    if (rostros.isNotEmpty()) {
                        val rostro = rostros[0]

                        Log.d("FaceDetection", "=== ROSTRO DETECTADO ===")
                        Log.d("FaceDetection", "Posición: X=${rostro.boundingBox.left}, Y=${rostro.boundingBox.top}")
                        Log.d("FaceDetection", "Ancho: ${rostro.boundingBox.width()}, Alto: ${rostro.boundingBox.height()}")

                        Log.d("FaceDetection", "--- PUNTOS DEL ROSTRO ---")

                        for (landmark in rostro.allLandmarks) {
                            val tipo = when (landmark.landmarkType) {
                                0 -> "OJO IZQUIERDO"
                                1 -> "OJO DERECHO"
                                2 -> "NARIZ"
                                3 -> "BOCA IZQUIERDA"
                                4 -> "BOCA DERECHA"
                                5 -> "BOCA CENTRO"
                                6 -> "OREJA IZQUIERDA"
                                7 -> "OREJA DERECHA"
                                8 -> "MEJILLA IZQUIERDA"
                                9 -> "MEJILLA DERECHA"
                                else -> "DESCONOCIDO"
                            }

                            val x = landmark.position.x.toInt()
                            val y = landmark.position.y.toInt()

                            Log.d("FaceDetection", "$tipo: ($x, $y)")
                        }

                        Log.d("FaceDetection", "Rotación Z: ${rostro.headEulerAngleZ}°")
                        Log.d("FaceDetection", "Rotación Y: ${rostro.headEulerAngleY}°")
                        Log.d("FaceDetection", "Distancia entre ojos: ${calcularDistanciaOjos(rostro)}")

                    } else {
                        Log.d("FaceDetection", "No se detectó rostro")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetection", "Error procesando imagen: ${e.message}")
                }
        }
    } catch (e: Exception) {
        Log.e("FaceDetection", "Error: ${e.message}")
    } finally {
        imageProxy.close()
    }
}

private fun calcularDistanciaOjos(rostro: com.google.mlkit.vision.face.Face): Float {
    val ojoIzquierdo = rostro.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)?.position
    val ojoDerecho = rostro.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)?.position

    return if (ojoIzquierdo != null && ojoDerecho != null) {
        val dx = ojoDerecho.x - ojoIzquierdo.x
        val dy = ojoDerecho.y - ojoIzquierdo.y
        kotlin.math.sqrt(dx * dx + dy * dy)
    } else {
        0f
    }
}