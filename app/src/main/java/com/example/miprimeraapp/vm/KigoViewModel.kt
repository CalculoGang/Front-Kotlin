package com.example.miprimeraapp.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.miprimeraapp.data.KigoRepository
import com.example.miprimeraapp.model.AppScreen
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.FaceUIState
import com.example.miprimeraapp.model.Persona
import com.example.miprimeraapp.model.TouchFormData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de pantalla/datos. `faceState` va aparte: se actualiza por-frame. */
data class KigoUiState(
    val screen   : AppScreen     = AppScreen.WELCOME,
    val form     : TouchFormData = TouchFormData(),
    val personas : List<Persona> = emptyList(),
    val empresas : List<Empresa> = emptyList()
)

/**
 * ViewModel único de la app (MVVM). Concentra el estado y la lógica de negocio que antes
 * vivían en MainActivity; el threading de red ocurre en [viewModelScope] vía [KigoRepository].
 */
class KigoViewModel(private val repo: KigoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(KigoUiState())
    val uiState: StateFlow<KigoUiState> = _uiState.asStateFlow()

    private val _faceState = MutableStateFlow(FaceUIState())
    val faceState: StateFlow<FaceUIState> = _faceState.asStateFlow()

    // Mensajes para Toast: la Activity los colecta y muestra (el VM no toca UI).
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    // Throttle del lookup biométrico (antes en MainActivity). Accedido desde el hilo del
    // analyzer y desde corrutinas -> @Volatile.
    @Volatile private var faceQueryInFlight = false
    @Volatile private var lastFaceQueryMs   = 0L
    @Volatile private var ultimoNombre      : String? = null
    @Volatile private var ultimaPersonaId   : String? = null

    init {
        viewModelScope.launch {
            val (per, emp) = repo.cargarCache()       // cache local primero (instantáneo)
            _uiState.update { it.copy(personas = per, empresas = emp) }
            runCatching { repo.refrescar() }          // luego refresco desde backend
                .onSuccess { (p, e) -> _uiState.update { s -> s.copy(personas = p, empresas = e) } }
                .onFailure { Log.e("KigoVM", "refresh falló: ${it.message}") }
        }
    }

    // ─── navegación / formulario ────────────────────────────────────────────
    fun navegar(screen: AppScreen) = _uiState.update { it.copy(screen = screen) }
    fun actualizarForm(form: TouchFormData) = _uiState.update { it.copy(form = form) }

    // ─── altas ──────────────────────────────────────────────────────────────
    fun addPersona(persona: Persona, muestras: List<List<Float>>) {
        if (persona.nombre.isBlank()) return
        if (muestras.isEmpty()) { toast("Captura al menos una muestra de rostro"); return }
        // persona.empresa trae el NOMBRE seleccionado; backend necesita el empresa_id (UUID).
        val empresaId = _uiState.value.empresas.find { it.nombre == persona.empresa }?.id
        if (empresaId.isNullOrBlank()) { toast("Empresa sin id de backend; sincroniza empresas primero"); return }
        viewModelScope.launch {
            runCatching { repo.crearPersona(persona, empresaId, muestras) }
                .onSuccess { per -> _uiState.update { it.copy(personas = per) } }
                .onFailure { toast("No se pudo crear persona: ${it.message}") }
        }
    }

    fun addEmpresa(empresa: Empresa) {
        if (empresa.nombre.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.crearEmpresa(empresa) }
                .onSuccess { creada -> _uiState.update { it.copy(empresas = it.empresas + creada) } }
                .onFailure { toast("No se pudo crear empresa: ${it.message}") }
        }
    }

    /** Registro rápido desde VoiceScreen: usa la primera empresa como destino por defecto. */
    fun savePerson(nombre: String, vector: List<Float>) {
        if (nombre.isBlank()) { toast("Escribe un nombre"); return }
        if (vector.isEmpty()) { toast("No hay rostro valido: acercate y mira de frente"); return }
        val empresaId = _uiState.value.empresas.firstOrNull()?.id
        if (empresaId.isNullOrBlank()) { toast("No hay empresa; crea una en Admin primero"); return }
        viewModelScope.launch {
            runCatching { repo.guardarRostro(nombre, empresaId, vector) }
                .onSuccess { per -> _uiState.update { it.copy(personas = per) }; toast("Rostro registrado: $nombre") }
                .onFailure { toast("No se pudo registrar: ${it.message}") }
        }
    }

    /** Añade el rostro actual como muestra extra de la persona ya reconocida (galería 1→N). */
    fun agregarMuestra() {
        val id = ultimaPersonaId
        val vector = _faceState.value.vector
        if (id.isNullOrBlank()) { toast("Primero deja que te reconozca"); return }
        if (vector.isEmpty())   { toast("No hay rostro valido: mira de frente"); return }
        viewModelScope.launch {
            runCatching { repo.agregarMuestra(id, vector) }
                .onSuccess { toast("Muestra añadida") }
                .onFailure { toast("No se pudo añadir muestra: ${it.message}") }
        }
    }

    // ─── pipeline facial (callbacks desde FacePipeline, hilo del analyzer) ─────
    /**
     * Recibe la detección cruda del pipeline. `vector` vacío = sin rostro apto.
     * El VM es dueño del nombre reconocido (lo pone el backend) y dispara el lookup.
     */
    fun onFaceDetected(hayRostro: Boolean, vector: List<Float>) {
        when {
            hayRostro && vector.isNotEmpty() -> {
                _faceState.value = FaceUIState(hayRostro = true, vector = vector, nombreReconocido = ultimoNombre)
                consultarRostro(vector)
            }
            hayRostro -> _faceState.value = FaceUIState(hayRostro = true, nombreReconocido = ultimoNombre)
            else -> {
                ultimoNombre = null; ultimaPersonaId = null
                _faceState.value = FaceUIState(hayRostro = false)
            }
        }
    }

    // Consulta el backend por el vector, con throttle (sin consultas solapadas).
    private fun consultarRostro(vector: List<Float>) {
        val now = System.currentTimeMillis()
        if (faceQueryInFlight || now - lastFaceQueryMs < FACE_QUERY_MS) return
        faceQueryInFlight = true
        lastFaceQueryMs   = now
        viewModelScope.launch {
            try {
                val persona = repo.buscarPorRostro(vector)
                ultimoNombre    = persona?.nombre
                ultimaPersonaId = persona?.id
                val s = _faceState.value
                if (s.hayRostro) _faceState.value = s.copy(nombreReconocido = ultimoNombre)
            } catch (e: Exception) {
                Log.e("KigoVM", "buscarPorRostro falló: ${e.message}")
            } finally {
                faceQueryInFlight = false
            }
        }
    }

    private fun toast(msg: String) { _toasts.tryEmit(msg) }

    companion object {
        private const val FACE_QUERY_MS = 1200L  // intervalo mínimo entre lookups al backend

        /** Factory manual (sin Hilt): inyecta el repositorio. */
        fun factory(repo: KigoRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = KigoViewModel(repo) as T
        }
    }
}
