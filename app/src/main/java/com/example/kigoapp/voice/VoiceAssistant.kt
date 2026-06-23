package com.example.kigoapp.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// TTS nativo elegido sobre Cloud TTS para funcionar offline, sin costo ni API key.
class VoiceAssistant(
    context: Context,
    private val onReady: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var listo = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Keyed by utterance ID; cleared on detener() so onFinish never fires on manual interruption
    private val pendingCallbacks = ConcurrentHashMap<String, () -> Unit>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Fallo al inicializar TTS (status=$status) — voz desactivada, flujo táctil sigue activo")
            return
        }

        val resultadoMX = tts?.setLanguage(Locale("es", "MX"))
        if (resultadoMX == TextToSpeech.LANG_MISSING_DATA || resultadoMX == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale es_MX no disponible, intentando es_ES")
            val resultadoES = tts?.setLanguage(Locale("es", "ES"))
            if (resultadoES == TextToSpeech.LANG_MISSING_DATA || resultadoES == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Locale es_ES tampoco disponible — voz desactivada, flujo táctil sigue activo")
                return
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            // onDone fires on the TTS service thread — remove the callback on the main thread
            // so that detener() (which clears the map on main) can always beat it and prevent
            // onFinish from firing after a manual interruption.
            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                mainHandler.post {
                    val cb = pendingCallbacks.remove(utteranceId) ?: return@post
                    cb()
                }
            }

            @Deprecated("Required override")
            override fun onError(utteranceId: String?) {
                if (utteranceId == null) return
                mainHandler.post { pendingCallbacks.remove(utteranceId) }
            }

            // API 23+: fires when stop() or QUEUE_FLUSH interrupts a pending utterance
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (utteranceId == null) return
                mainHandler.post { pendingCallbacks.remove(utteranceId) }
            }
        })

        seleccionarMejorVozLocal()
        listo = true
        Log.d(TAG, "TTS inicializado correctamente")
        mainHandler.post(onReady)
    }

    private fun seleccionarMejorVozLocal() {
        val voces = tts?.voices ?: return
        // Preferir voces locales (sin red) en español, ordenadas por calidad descendente
        val mejor = voces
            .filter { !it.isNetworkConnectionRequired && it.locale.language == "es" }
            .maxByOrNull { it.quality }
        if (mejor != null) {
            tts?.voice = mejor
            Log.d(TAG, "Voz local seleccionada: ${mejor.name} (calidad=${mejor.quality})")
        }
    }

    fun reproducirMensaje(numeroMensaje: Int) =
        reproducirMensaje(numeroMensaje, {})

    fun reproducirMensaje(numeroMensaje: Int, onFinish: () -> Unit) {
        val texto = CATALOGO[numeroMensaje]
        if (texto == null) {
            Log.w(TAG, "Mensaje #$numeroMensaje no existe en el catálogo")
            return
        }
        if (!listo) {
            Log.w(TAG, "TTS aún no está listo — omitiendo mensaje #$numeroMensaje")
            return
        }
        val utteranceId = "msg_$numeroMensaje"
        pendingCallbacks[utteranceId] = onFinish
        // QUEUE_FLUSH interrumpe cualquier audio en curso; en un kiosko no deben encolarse mensajes
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /** Habla un texto libre (ej. "Hola Juan") sin pasar por el catálogo. */
    fun reproducirTexto(texto: String, utteranceId: String, onFinish: () -> Unit = {}) {
        if (!listo) {
            Log.w(TAG, "TTS aún no está listo — omitiendo texto libre")
            return
        }
        pendingCallbacks[utteranceId] = onFinish
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun detener() {
        // Clear callbacks before stop() so the onStop listener discards them, not onDone
        pendingCallbacks.clear()
        tts?.stop()
    }

    fun shutdown() {
        pendingCallbacks.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        listo = false
    }

    companion object {
        private const val TAG = "VoiceAssistant"

        val CATALOGO: Map<Int, String> = mapOf(
            1  to "¡Bienvenido a tu comunidad! Soy Kigo, tu asistente de registro. ¿En qué idioma prefieres continuar?",
            2  to "¿Cómo deseas interactuar hoy? Puedes tocar la pantalla o puedes hablarme directamente.",
            3  to "¿Vienes como visitante o como proveedor de servicios?",
            4  to "¿Cuál es tu nombre completo?",
            5  to "¿Cuál es tu nombre completo y de qué empresa vienes hoy?",
            6  to "No pude escucharte bien. ¿Puedes repetirlo o escribirlo en la pantalla?",
            7  to "¿Cuál es el motivo de tu visita hoy?",
            8  to "¿A quién vas a visitar? Dime el nombre del residente o persona que te espera.",
            9  to "No encontré ese nombre en el directorio. ¿Puedes confirmarlo e intentarlo de nuevo?",
            10 to "¿A qué área o departamento te diriges?",
            11 to "Ahora necesito tomarte una fotografía. Por favor, mira directamente a la cámara y mantente quieto.",
            12 to "No detecto tu rostro. Acércate un poco más y mira hacia el centro de la cámara.",
            13 to "La imagen no está clara. Busca buena iluminación e intenta de nuevo.",
            14 to "Fotografía capturada correctamente.",
            15 to "¿Qué tipo de identificación traes contigo? INE, pasaporte o licencia de conducir.",
            16 to "Coloca tu identificación frente a la cámara. Asegúrate de que se vea completa y que la información sea legible.",
            17 to "El documento parece estar cortado. Céntralo bien dentro del recuadro e intenta de nuevo.",
            18 to "La imagen salió borrosa. Mantén la identificación firme e intenta nuevamente.",
            19 to "La iluminación no es suficiente. Busca un área más iluminada e intenta de nuevo.",
            20 to "Un momento, estoy procesando y leyendo tu identificación.",
            21 to "No pude leer bien tu identificación. Por favor, verifica y corrige tus datos directamente en pantalla.",
            22 to "Confirmemos que tus datos son correctos. ¿Todo está correcto?",
            23 to "Sin problema, corrige los datos directamente en la pantalla.",
            24 to "Validando tu acceso con el sistema Kigo.",
            25 to "Tu acceso ha sido autorizado. El anfitrión ha sido notificado de tu llegada.",
            26 to "Tu solicitud está siendo revisada. Hemos notificado al personal. Por favor, espera un momento aquí.",
            27 to "No es posible autorizar el acceso en este momento. Comunícate con el personal de seguridad.",
            28 to "Gracias por usar Kigo. Que tengas un excelente día.",
            29 to "Hemos registrado el motivo de tu visita, a quién visitas y el área de destino. En la siguiente fase tomaremos tu fotografía y tu identificación."
        )
    }
}
