package com.example.kigoapp.flow

// Pure Kotlin — no android.* imports — testeable sin emulador ni contexto

enum class ConversationStep { MOTIVO, ANFITRION, AREA, CONFIRMACION, COMPLETADO }

data class VoiceFlowData(
    val motivo: String = "",
    val anfitrionNombre: String = "",
    val anfitrionDireccion: String = "",
    val area: String = ""
)

data class Residente(val nombre: String, val direccion: String)

// TODO: reemplazar por consulta real al backend Go cuando exista el endpoint de directorio.
val DIRECTORIO_MOCK = listOf(
    Residente("David",        "Torre A, depto 502"),
    Residente("Juan Pérez",   "Torre B, depto 210"),
    Residente("María García", "Torre A, depto 118")
)

sealed class EngineResult {
    // Common fields lifted to the sealed class so callers avoid repetitive `when`
    abstract val numeroMensaje  : Int
    abstract val datos          : VoiceFlowData
    abstract val siguienteEstado: ConversationStep

    data class Hablar(
        override val numeroMensaje  : Int,
        override val datos          : VoiceFlowData,
        override val siguienteEstado: ConversationStep
    ) : EngineResult()

    data class AnfitrionEncontrado(
        val residente            : Residente,
        override val numeroMensaje  : Int,
        override val datos          : VoiceFlowData,
        override val siguienteEstado: ConversationStep
    ) : EngineResult()
}

object DialogEngine {

    fun procesarRespuesta(
        estadoActual : ConversationStep,
        datosActuales: VoiceFlowData,
        textoUsuario : String
    ): EngineResult = when (estadoActual) {

        ConversationStep.MOTIVO -> EngineResult.Hablar(
            numeroMensaje   = 8,
            datos           = datosActuales.copy(motivo = textoUsuario),
            siguienteEstado = ConversationStep.ANFITRION
        )

        ConversationStep.ANFITRION -> {
            val texto = textoUsuario.lowercase()
            val encontrado = DIRECTORIO_MOCK.firstOrNull { res ->
                val nombre = res.nombre.lowercase()
                nombre.contains(texto) || texto.contains(nombre)
            }
            if (encontrado != null) {
                EngineResult.AnfitrionEncontrado(
                    residente       = encontrado,
                    numeroMensaje   = 10,
                    datos           = datosActuales.copy(
                        anfitrionNombre    = encontrado.nombre,
                        anfitrionDireccion = encontrado.direccion
                    ),
                    siguienteEstado = ConversationStep.AREA
                )
            } else {
                EngineResult.Hablar(
                    numeroMensaje   = 9,
                    datos           = datosActuales,          // stay — no state advance
                    siguienteEstado = ConversationStep.ANFITRION
                )
            }
        }

        ConversationStep.AREA -> EngineResult.Hablar(
            numeroMensaje   = 22,
            datos           = datosActuales.copy(area = textoUsuario),
            siguienteEstado = ConversationStep.CONFIRMACION
        )

        ConversationStep.CONFIRMACION -> {
            val norm = textoUsuario.lowercase()
            val afirmacion = listOf("sí", "si", "correcto", "así es", "perfecto", "exacto")
            val negacion   = listOf("no", "incorrecto", "mal", "está mal")
            when {
                afirmacion.any { norm.contains(it) } -> EngineResult.Hablar(
                    numeroMensaje   = 29,
                    datos           = datosActuales,
                    siguienteEstado = ConversationStep.COMPLETADO
                )
                negacion.any { norm.contains(it) } -> {
                    // TODO: mejora futura — corregir campo por campo en vez de reiniciar todo
                    EngineResult.Hablar(
                        numeroMensaje   = 7,
                        datos           = VoiceFlowData(),   // reset completo
                        siguienteEstado = ConversationStep.MOTIVO
                    )
                }
                else -> EngineResult.Hablar(
                    numeroMensaje   = 6,
                    datos           = datosActuales,          // stay — no state advance
                    siguienteEstado = ConversationStep.CONFIRMACION
                )
            }
        }

        ConversationStep.COMPLETADO -> EngineResult.Hablar(
            numeroMensaje   = 29,
            datos           = datosActuales,
            siguienteEstado = ConversationStep.COMPLETADO    // idempotent
        )
    }
}
