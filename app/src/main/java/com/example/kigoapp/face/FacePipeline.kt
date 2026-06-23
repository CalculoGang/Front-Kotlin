package com.example.kigoapp.face

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import java.util.concurrent.Executor

/**
 * Pipeline de cámara + reconocimiento facial extraído de MainActivity.
 *
 * Detecta el rostro más grande, filtra por calidad, genera el embedding (promediado sobre
 * los últimos N frames buenos) y lo emite vía [onFace]. NO consulta el backend: eso lo
 * decide el ViewModel. `vector` vacío en [onFace] = hay cara pero no apta / no hay cara.
 *
 * @param onFace (hayRostro, vector) en el hilo de [analyzerExecutor].
 */
class FacePipeline(
    private val embedder: FaceEmbedder?,
    private val detector: FaceDetector,
    private val analyzerExecutor: Executor,
    private val onFace: (hayRostro: Boolean, vector: List<Float>) -> Unit
) {
    // Últimos N embeddings buenos para promediar (un frame suelto es ruidoso). Solo se toca
    // desde el callback del analyzer (hilo único) -> sin lock.
    private val vectorBuffer = ArrayDeque<List<Float>>()
    @Volatile private var lastDbgMs = 0L

    @SuppressLint("UnsafeOptInUsageError")
    fun setup(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val context = previewView.context
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analyzerExecutor, ::processFrame) }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Log.e("Camera", "Error configurando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image      = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener(analyzerExecutor) { rostros ->
                val emb = embedder
                // Cara más grande, no la primera: la más cercana es la que va a interactuar.
                val rostro = rostros.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                // ponytail: diagnóstico temporal, quitar cuando el reconocimiento esté afinado.
                if (rostro != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastDbgMs > 1000) {
                        lastDbgMs = now
                        val b = rostro.boundingBox
                        Log.d("FacePipe", "caras=${rostros.size} box=${b.width()}x${b.height()} " +
                            "yaw=${rostro.headEulerAngleY.toInt()} roll=${rostro.headEulerAngleZ.toInt()} " +
                            "apta=${esCalidadSuficiente(rostro)} (minPx=$FACE_MIN_PX maxYaw=$FACE_MAX_YAW_DEG)")
                    }
                } else if (rostros.isEmpty() && System.currentTimeMillis() - lastDbgMs > 1000) {
                    lastDbgMs = System.currentTimeMillis()
                    Log.d("FacePipe", "sin rostro detectado")
                }
                if (rostro != null && emb != null && esCalidadSuficiente(rostro)) {
                    try {
                        val bmp  = ImageUtils.rotate(ImageUtils.toBitmap(imageProxy), imageProxy.imageInfo.rotationDegrees)
                        // Alinear (nivelar ojos por el roll) + margen: MobileFaceNet embebe mal un box crudo.
                        val face = ImageUtils.alignFace(bmp, rostro.boundingBox, rostro.headEulerAngleZ, emb.inputSize)
                        if (face != null) {
                            val v = emb.embed(face).toList()
                            vectorBuffer.addLast(v)
                            while (vectorBuffer.size > FACE_AVG_FRAMES) vectorBuffer.removeFirst()
                            // Promedio de los últimos N frames buenos -> menos ruido que un frame suelto.
                            val vector = FaceRecognitionEngine.promediar(vectorBuffer) ?: v
                            onFace(true, vector)
                        }
                    } catch (e: Exception) {
                        Log.e("FaceEmbed", "Error generando embedding: ${e.message}")
                    }
                } else {
                    // Hay cara pero no apta (lejos/de perfil) o no hay cara -> sin vector.
                    vectorBuffer.clear()  // no mezclar frames entre personas/sesiones
                    onFace(rostro != null, emptyList())
                }
            }
            .addOnFailureListener { Log.e("FaceDetect", "Error: ${it.message}") }
            .addOnCompleteListener { imageProxy.close() }
    }

    // Solo embebemos caras frontales y suficientemente grandes; una cara de perfil o lejana
    // produce un vector basura que ensucia el match.
    private fun esCalidadSuficiente(rostro: Face): Boolean {
        val box = rostro.boundingBox
        val grande  = box.width() >= FACE_MIN_PX && box.height() >= FACE_MIN_PX
        val frontal = kotlin.math.abs(rostro.headEulerAngleY) <= FACE_MAX_YAW_DEG &&
                      kotlin.math.abs(rostro.headEulerAngleZ) <= FACE_MAX_ROLL_DEG
        return grande && frontal
    }

    companion object {
        // Gates de calidad de rostro. ponytail: tuneables; sube FACE_MIN_PX si el kiosko
        // está lejos del usuario, baja los grados si entran demasiados perfiles.
        private const val FACE_MIN_PX       = 100   // lado mínimo del box en px
        private const val FACE_MAX_YAW_DEG  = 18f   // giro horizontal (perfil) máximo
        private const val FACE_MAX_ROLL_DEG = 18f   // inclinación lateral máxima
        private const val FACE_AVG_FRAMES   = 5     // frames buenos a promediar por emisión
    }
}
