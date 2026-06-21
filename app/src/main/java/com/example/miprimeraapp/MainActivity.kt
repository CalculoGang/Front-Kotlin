package com.example.miprimeraapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.miprimeraapp.data.KigoRepository
import com.example.miprimeraapp.face.FacePipeline
import com.example.miprimeraapp.vm.KigoViewModel
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Cascarón de la app (MVVM): cablea el [KigoViewModel] con el [FacePipeline] y la UI Compose.
 * Estado y lógica de negocio viven en el ViewModel; la cámara/embedding en el pipeline.
 */
class MainActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null
    private var faceEmbedder: FaceEmbedder? = null
    private lateinit var facePipeline: FacePipeline

    private val viewModel: KigoViewModel by viewModels {
        KigoViewModel.factory(KigoRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        faceEmbedder = try {
            FaceEmbedder(this)
        } catch (e: Exception) {
            Log.e("FaceEmbedder", "Error cargando modelo .tflite: ${e.message}"); null
        }

        faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        facePipeline = FacePipeline(faceEmbedder, faceDetector!!, cameraExecutor, viewModel::onFaceDetected)

        requestCameraPermission()

        // Toasts del ViewModel (el VM no toca UI; la Activity es dueña del Toast).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toasts.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
            }
        }

        setContent {
            val ui   by viewModel.uiState.collectAsStateWithLifecycle()
            val face by viewModel.faceState.collectAsStateWithLifecycle()
            KigoApp(
                currentScreen    = ui.screen,
                onNavigate       = viewModel::navegar,
                faceUIState      = face,
                touchFormData    = ui.form,
                onFormUpdate     = viewModel::actualizarForm,
                onGuardarFace    = viewModel::savePerson,
                onAgregarMuestra = viewModel::agregarMuestra,
                onSetupCamera    = { facePipeline.setup(this, it) },
                personas         = ui.personas,
                empresas         = ui.empresas,
                onAddPersona     = viewModel::addPersona,
                onAddEmpresa     = viewModel::addEmpresa
            )
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
