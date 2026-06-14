package com.example.miprimeraapp.model

enum class AppScreen { WELCOME, MODE_SELECT, TOUCH_FORM, VOICE, SUCCESS, SPEECH_TEST }

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
