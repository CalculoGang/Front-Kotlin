package com.example.miprimeraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.miprimeraapp.model.AppScreen
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.FaceUIState
import com.example.miprimeraapp.model.Persona
import com.example.miprimeraapp.model.TouchFormData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val personasDB     = ConcurrentHashMap<String, MutableList<List<Float>>>()
    private val faceUIState    = mutableStateOf(FaceUIState())
    private val currentScreen  = mutableStateOf(AppScreen.WELCOME)
    private val touchFormData  = mutableStateOf(TouchFormData())
    private val personas       = mutableStateOf<List<Persona>>(emptyList())
    private val empresas       = mutableStateOf<List<Empresa>>(emptyList())

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val netExecutor    = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null
    private var faceEmbedder: FaceEmbedder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FaceStorage.cargar(this).forEach { (nombre, vectores) ->
            personasDB[nombre] = vectores
        }

        // Cache local primero (display instantaneo), luego refresco desde backend.
        personas.value = AdminStorage.cargarPersonas(this)
        empresas.value = AdminStorage.cargarEmpresas(this)
        refrescarDesdeBackend()

        try {
            faceEmbedder = FaceEmbedder(this)
        } catch (e: Exception) {
            Log.e("FaceEmbedder", "Error cargando modelo .tflite: ${e.message}")
        }

        faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        requestCameraPermission()

        setContent {
            KigoApp(
                currentScreen  = currentScreen.value,
                onNavigate     = { currentScreen.value = it },
                faceUIState    = faceUIState.value,
                touchFormData  = touchFormData.value,
                onFormUpdate   = { touchFormData.value = it },
                onGuardarFace  = ::savePerson,
                onSetupCamera  = ::setupCamera,
                personas       = personas.value,
                empresas       = empresas.value,
                onAddPersona   = ::addPersona,
                onAddEmpresa   = ::addEmpresa
            )
        }
    }

    private fun refrescarDesdeBackend() {
        netExecutor.execute {
            try {
                val emp = KigoApi.listEmpresas()
                val per = KigoApi.listPersonas()
                runOnUiThread {
                    empresas.value = emp
                    personas.value = per
                    AdminStorage.guardar(this, per, emp)
                }
            } catch (e: Exception) {
                Log.e("KigoApi", "refresh fallo: ${e.message}")
            }
        }
    }

    // vector = 192 floats biometricos capturados en AdminScreen (puede ir vacio).
    private fun addPersona(persona: Persona, vector: List<Float>) {
        if (persona.nombre.isBlank()) return
        // persona.empresa trae el NOMBRE seleccionado; backend necesita el empresa_id (UUID).
        val empresaId = empresas.value.find { it.nombre == persona.empresa }?.id
        if (empresaId.isNullOrBlank()) {
            toast("Empresa sin id de backend; sincroniza empresas primero")
            return
        }
        netExecutor.execute {
            try {
                KigoApi.createPersona(persona, empresaId, vector)
                val per = KigoApi.listPersonas()
                runOnUiThread {
                    personas.value = per
                    AdminStorage.guardar(this, personas.value, empresas.value)
                }
            } catch (e: Exception) {
                Log.e("KigoApi", "createPersona fallo: ${e.message}")
                runOnUiThread { toast("No se pudo crear persona: ${e.message}") }
            }
        }
    }

    private fun addEmpresa(empresa: Empresa) {
        if (empresa.nombre.isBlank()) return
        netExecutor.execute {
            try {
                val creada = KigoApi.createEmpresa(empresa)
                runOnUiThread {
                    empresas.value = empresas.value + creada
                    AdminStorage.guardar(this, personas.value, empresas.value)
                }
            } catch (e: Exception) {
                Log.e("KigoApi", "createEmpresa fallo: ${e.message}")
                runOnUiThread { toast("No se pudo crear empresa: ${e.message}") }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun savePerson(nombre: String, vector: List<Float>) {
        if (nombre.isBlank() || vector.isEmpty()) return
        personasDB.getOrPut(nombre) { mutableListOf() }.add(vector)
        FaceStorage.guardar(this, personasDB)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, ::processFrame) }

                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Log.e("Camera", "Error configurando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image      = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector?.process(image)
            ?.addOnSuccessListener(cameraExecutor) { rostros ->
                val embedder = faceEmbedder
                if (rostros.isNotEmpty() && embedder != null) {
                    try {
                        val bmp  = ImageUtils.rotate(ImageUtils.toBitmap(imageProxy), imageProxy.imageInfo.rotationDegrees)
                        val face = ImageUtils.cropFace(bmp, rostros[0].boundingBox)
                        if (face != null) {
                            val vector          = embedder.embed(face).toList()
                            val (nombre, dist)  = FaceRecognitionEngine.reconocer(vector, personasDB)
                            faceUIState.value   = FaceUIState(hayRostro = true, vector = vector, nombreReconocido = nombre, distancia = dist)
                        }
                    } catch (e: Exception) {
                        Log.e("FaceEmbed", "Error generando embedding: ${e.message}")
                    }
                } else {
                    faceUIState.value = FaceUIState(hayRostro = false)
                }
            }
            ?.addOnFailureListener { Log.e("FaceDetect", "Error: ${it.message}") }
            ?.addOnCompleteListener { imageProxy.close() }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        netExecutor.shutdown()
        faceDetector?.close()
        faceEmbedder?.close()
    }
}
