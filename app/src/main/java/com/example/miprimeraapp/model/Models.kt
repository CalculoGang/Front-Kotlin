package com.example.miprimeraapp.model

enum class AppScreen { WELCOME, MODE_SELECT, TOUCH_FORM, VOICE, SUCCESS, SPEECH_TEST, ADMIN }

// Mapea tabla `empresas` del backend (sin id/timestamps — los pone el backend).
data class Empresa(
    val nombre           : String  = "",
    val tipo             : String  = "",
    val direccion        : String  = "",
    val correoContacto   : String  = "",
    val telefonoContacto : String  = "",
    val activa           : Boolean = true
)

// Mapea tabla `personas` + campos dinamicos (`persona_campos`: telefono, correo).
// `empresa` referencia la empresa dueña (empresa_id); local guarda el nombre.
data class Persona(
    val nombre             : String = "",
    val tipo               : String = "visitante", // 'visitante' | 'proveedor' | 'empleado'
    val empresa            : String = "",          // empresa_id (local: nombre de empresa)
    val empresaOrigen      : String = "",
    val tipoIdentificacion : String = "",
    val telefono           : String = "",          // persona_campos
    val correo             : String = ""           // persona_campos
)

data class TouchFormData(
    val nombre      : String = "",
    val empresa     : String = "",
    val telefono    : String = "",
    val motivo      : String = "",
    val contacto    : String = "",
    val tipoVisita  : String = "",
    val tipoId      : String = "",
    val observaciones: String = "",
    val step        : Int    = 1
)

data class FaceUIState(
    val hayRostro        : Boolean      = false,
    val vector           : List<Float>  = emptyList(),
    val nombreReconocido : String?      = null,
    val distancia        : Float        = Float.MAX_VALUE
)
