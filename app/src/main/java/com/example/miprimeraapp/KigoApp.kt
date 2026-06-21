package com.example.miprimeraapp

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.miprimeraapp.model.AppScreen
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.FaceUIState
import com.example.miprimeraapp.model.Persona
import com.example.miprimeraapp.model.TouchFormData
import com.example.miprimeraapp.ui.screens.*

@Composable
fun KigoApp(
    currentScreen : AppScreen,
    onNavigate    : (AppScreen) -> Unit,
    faceUIState   : FaceUIState,
    touchFormData : TouchFormData,
    onFormUpdate  : (TouchFormData) -> Unit,
    onGuardarFace : (String, List<Float>) -> Unit,
    onSetupCamera : (PreviewView) -> Unit,
    personas      : List<Persona>,
    empresas      : List<Empresa>,
    onAddPersona  : (Persona) -> Unit,
    onAddEmpresa  : (Empresa) -> Unit
) {
    when (currentScreen) {
        AppScreen.WELCOME     -> WelcomeScreen(
            onStart      = { onNavigate(AppScreen.MODE_SELECT) },
            onTestSpeech = { onNavigate(AppScreen.SPEECH_TEST) },
            onAdmin      = { onNavigate(AppScreen.ADMIN) }
        )
        AppScreen.MODE_SELECT -> ModeSelectScreen(
            onBack        = { onNavigate(AppScreen.WELCOME) },
            onSelectTouch = { onNavigate(AppScreen.TOUCH_FORM) },
            onSelectVoice = { onNavigate(AppScreen.VOICE) }
        )
        AppScreen.TOUCH_FORM  -> TouchFormScreen(
            formData = touchFormData,
            onUpdate = onFormUpdate,
            onBack   = { onNavigate(AppScreen.MODE_SELECT) },
            onSubmit = { onNavigate(AppScreen.SUCCESS) }
        )
        AppScreen.VOICE       -> VoiceScreen(
            faceUIState   = faceUIState,
            onGuardarFace = onGuardarFace,
            onSetupCamera = onSetupCamera,
            onBack        = { onNavigate(AppScreen.MODE_SELECT) }
        )
        AppScreen.SUCCESS     -> SuccessScreen(
            formData = touchFormData,
            onReset  = {
                onFormUpdate(TouchFormData())
                onNavigate(AppScreen.WELCOME)
            }
        )
        AppScreen.SPEECH_TEST -> SpeechTestScreen(
            onBack = { onNavigate(AppScreen.WELCOME) }
        )
        AppScreen.ADMIN       -> AdminScreen(
            personas     = personas,
            empresas     = empresas,
            onAddPersona = onAddPersona,
            onAddEmpresa = onAddEmpresa,
            onBack       = { onNavigate(AppScreen.WELCOME) }
        )
    }
}
