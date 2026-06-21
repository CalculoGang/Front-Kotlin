package com.example.miprimeraapp.voice

interface VoiceOutput {
    fun reproducirMensaje(numeroMensaje: Int)
    fun reproducirMensaje(numeroMensaje: Int, onFinish: () -> Unit)
    fun detener()
}
