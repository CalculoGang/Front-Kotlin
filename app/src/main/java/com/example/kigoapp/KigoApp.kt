package com.example.kigoapp

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.kigoapp.model.AppScreen
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.model.FaceUIState
import com.example.kigoapp.model.Persona
import com.example.kigoapp.model.TouchFormData
import com.example.kigoapp.ui.screens.*

@Composable
fun KigoApp(
    currentScreen : AppScreen,
    onNavigate    : (AppScreen) -> Unit,
    faceUIState   : FaceUIState,
    touchFormData : TouchFormData,
    onFormUpdate  : (TouchFormData) -> Unit,
    onGuardarFace : (String, List<Float>) -> Unit,
    onAgregarMuestra : () -> Unit,
    onSetupCamera : (PreviewView) -> Unit,
    personas      : List<Persona>,
    empresas      : List<Empresa>,
    onAddPersona  : (Persona, List<List<Float>>) -> Unit,
    onAddEmpresa  : (Empresa) -> Unit,
    backendOnline : Boolean?  = null
) {
    when (currentScreen) {
        AppScreen.WELCOME     -> WelcomeScreen(
            onStart       = { onNavigate(AppScreen.MODE_SELECT) },
            onAdmin       = { onNavigate(AppScreen.ADMIN) },
            backendOnline = backendOnline
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
            faceUIState      = faceUIState,
            onGuardarFace    = onGuardarFace,
            onAgregarMuestra = onAgregarMuestra,
            onSetupCamera    = onSetupCamera,
            onBack           = { onNavigate(AppScreen.MODE_SELECT) }
        )
        AppScreen.SUCCESS     -> SuccessScreen(
            formData = touchFormData,
            onReset  = {
                onFormUpdate(TouchFormData())
                onNavigate(AppScreen.WELCOME)
            }
        )
        AppScreen.ADMIN       -> AdminScreen(
            personas      = personas,
            empresas      = empresas,
            faceUIState   = faceUIState,
            onSetupCamera = onSetupCamera,
            onAddPersona  = onAddPersona,
            onAddEmpresa  = onAddEmpresa,
            onBack        = { onNavigate(AppScreen.WELCOME) }
        )
    }
}
