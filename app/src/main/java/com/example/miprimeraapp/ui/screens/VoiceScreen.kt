package com.example.miprimeraapp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.miprimeraapp.R
import com.example.miprimeraapp.model.FaceUIState
import com.example.miprimeraapp.ui.components.*
import com.example.miprimeraapp.ui.theme.KigoColors
import com.example.miprimeraapp.flow.ConversationStep
import com.example.miprimeraapp.flow.DialogEngine
import com.example.miprimeraapp.flow.EngineResult
import com.example.miprimeraapp.flow.VoiceFlowData
import com.example.miprimeraapp.voice.VoiceAssistant
import java.util.Locale

@Composable
fun VoiceScreen(
    faceUIState   : FaceUIState,
    onGuardarFace : (String, List<Float>) -> Unit,
    onSetupCamera : (PreviewView) -> Unit,
    onBack        : () -> Unit
) {
    val context            = LocalContext.current
    var micActive          by remember { mutableStateOf(false) }
    var kigoHablando       by remember { mutableStateOf(false) }
    var asistente          by remember { mutableStateOf<VoiceAssistant?>(null) }
    var estadoConversacion by remember { mutableStateOf(ConversationStep.MOTIVO) }
    var datosCapturados    by remember { mutableStateOf(VoiceFlowData()) }
    var mostrarDialogo     by remember { mutableStateOf(false) }
    var nombreInput        by remember { mutableStateOf("") }
    var hasPermission  by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>(
            // Texto tomado del catálogo: pantalla y audio nunca pueden desincronizarse
            ChatMessage.Kigo(VoiceAssistant.CATALOGO[7] ?: "")
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // Defined before the DisposableEffect blocks so that onReady / onFinish can reference them
    fun startListening() {
        if (!hasPermission) { permLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,       Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,    3)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer.stopListening()
        micActive = false
    }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { micActive = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                micActive = false
                messages.removeAll { it is ChatMessage.UserPartial }
            }
            override fun onResults(results: Bundle?) {
                micActive = false
                messages.removeAll { it is ChatMessage.UserPartial }
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.isNullOrEmpty()) return

                val texto = matches[0]
                messages.add(ChatMessage.User(texto))
                messages.add(ChatMessage.Typing)

                val resultado = DialogEngine.procesarRespuesta(estadoConversacion, datosCapturados, texto)
                datosCapturados    = resultado.datos
                estadoConversacion = resultado.siguienteEstado

                messages.remove(ChatMessage.Typing)

                if (resultado is EngineResult.AnfitrionEncontrado) {
                    messages.add(ChatMessage.ResidentFound(
                        name    = resultado.residente.nombre,
                        address = resultado.residente.direccion
                    ))
                }
                messages.add(ChatMessage.Kigo(VoiceAssistant.CATALOGO[resultado.numeroMensaje] ?: ""))

                stopListening()
                kigoHablando = true
                asistente?.reproducirMensaje(resultado.numeroMensaje, onFinish = {
                    kigoHablando = false
                    if (estadoConversacion != ConversationStep.COMPLETADO) startListening()
                })
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty() && partial[0].isNotBlank()) {
                    messages.removeAll { it is ChatMessage.UserPartial }
                    messages.add(ChatMessage.UserPartial(partial[0]))
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    DisposableEffect(Unit) {
        // var + nullable ref avoids a chicken-and-egg capture problem: onReady fires
        // asynchronously (after TTS init), so `va` is guaranteed to be assigned by then.
        var va: VoiceAssistant? = null
        va = VoiceAssistant(context, onReady = {
            // Kigo opens the conversation — mic must be silent while Kigo speaks
            stopListening()
            kigoHablando = true
            va?.reproducirMensaje(7, onFinish = {
                // Kigo finished speaking naturally → reactivate mic automatically
                kigoHablando = false
                startListening()
            })
        })
        asistente = va
        onDispose { va?.shutdown() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.AppBg)
    ) {
        KigoAppleHeader()

        // Upper zone: camera (left), background space (right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PortraitCameraBlock(
                faceUIState   = faceUIState,
                onSetupCamera = onSetupCamera,
                onRegisterTap = {
                    nombreInput    = faceUIState.nombreReconocido ?: ""
                    mostrarDialogo = true
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 8.dp)
                    .width(155.dp)
                    .fillMaxHeight(0.92f)
            )
        }

        // Chat zone: mascot floats above card, glass card fills zone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        ) {
            GlassChatCard(
                messages     = messages,
                quickActions = listOf("Llamar a David", "Cancelar visita"),
                modifier     = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            )

            // Drawn after card → renders on top; negative Y offset floats into camera zone above
            MascotImage(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 30.dp)
                    .offset(y = (-185).dp)
                    .size(215.dp)
                    .zIndex(5f)
            )
        }

        AppleBottomNav(
            micActive   = micActive,
            onMicToggle = {
                if (micActive) {
                    stopListening()
                } else {
                    // If Kigo is currently speaking, stop audio before activating mic —
                    // both cannot run simultaneously or the mic captures Kigo's own voice
                    if (kigoHablando) {
                        kigoHablando = false   // clear flag before detener() discards callbacks
                        asistente?.detener()
                    }
                    startListening()
                }
            },
            onHome      = onBack,
            modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (mostrarDialogo) {
        RegisterFaceDialog(
            nombreInput  = nombreInput,
            onNameChange = { nombreInput = it },
            onConfirm    = { onGuardarFace(nombreInput, faceUIState.vector); mostrarDialogo = false },
            onDismiss    = { mostrarDialogo = false }
        )
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun KigoAppleHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text          = "KIGO",
                fontSize      = 22.sp,
                fontWeight    = FontWeight.Bold,
                color         = KigoColors.KigoRed,
                letterSpacing = (-0.5).sp
            )
            Text(
                text          = "LAS CUMBRES",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = Color(0xFF9CA3AF),
                letterSpacing = 1.8.sp
            )
        }
    }
}

// ─── Portrait camera block ────────────────────────────────────────────────────

@Composable
private fun PortraitCameraBlock(
    faceUIState   : FaceUIState,
    onSetupCamera : (PreviewView) -> Unit,
    onRegisterTap : () -> Unit,
    modifier      : Modifier = Modifier
) {
    val recTransition = rememberInfiniteTransition(label = "rec")
    val recAlpha by recTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recDot"
    )

    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(alpha = 0.22f))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF17172A))
    ) {
        CameraPreview(onSetupCamera = onSetupCamera)

        // Face recognition status chip (tap to register face)
        val (chipColor, chipText) = when {
            !faceUIState.hayRostro               -> Pair(Color.Black.copy(alpha = 0.6f), "Sin rostro")
            faceUIState.nombreReconocido != null -> Pair(KigoColors.VoiceGreen.copy(alpha = 0.85f), faceUIState.nombreReconocido!!)
            else                                 -> Pair(KigoColors.KigoRed.copy(alpha = 0.85f), "Desconocido")
        }

        if (faceUIState.hayRostro) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(chipColor)
                    .clickable(onClick = onRegisterTap)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(chipText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom bar: LIVE pulsing dot
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(KigoColors.KigoRed.copy(alpha = recAlpha), CircleShape)
                )
                Text(
                    text          = "LIVE",
                    color         = Color.White.copy(alpha = 0.38f),
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(3.dp))
            )
        }
    }
}

// ─── Mascot image with float animation ───────────────────────────────────────

@Composable
private fun MascotImage(modifier: Modifier = Modifier) {
    Image(
        painter            = painterResource(R.drawable.dog_chat),
        contentDescription = "KIGO",
        contentScale       = ContentScale.Fit,
        modifier           = modifier
    )
}

// ─── Glass chat card ──────────────────────────────────────────────────────────

@Composable
private fun GlassChatCard(
    messages     : List<ChatMessage>,
    quickActions : List<String>,
    modifier     : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.07f))
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.88f))
    ) {
        ConversationList(
            messages = messages,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActions.forEach { label ->
                QuickActionChip(label = label, onClick = {})
            }
        }
    }
}

// ─── Bottom navigation ────────────────────────────────────────────────────────

@Composable
private fun AppleBottomNav(
    micActive   : Boolean,
    onMicToggle : () -> Unit,
    onHome      : () -> Unit,
    modifier    : Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(999.dp), ambientColor = Color.Black.copy(alpha = 0.07f))
            .background(Color.White.copy(alpha = 0.76f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(KigoColors.KigoRed, CircleShape)
                .clickable(onClick = onHome),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Home,
                contentDescription = "Inicio",
                tint               = Color.White,
                modifier           = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-10).dp)
                .shadow(
                    elevation    = if (micActive) 16.dp else 4.dp,
                    shape        = CircleShape,
                    ambientColor = KigoColors.KigoRed.copy(alpha = if (micActive) 0.46f else 0.1f)
                )
                .background(KigoColors.KigoRed, CircleShape)
                .clickable(onClick = onMicToggle),
            contentAlignment = Alignment.Center
        ) {
            if (micActive) {
                WaveformBars()
            } else {
                Icon(
                    imageVector        = Icons.Filled.Mic,
                    contentDescription = "Microfono",
                    tint               = Color.White,
                    modifier           = Modifier.size(26.dp)
                )
            }
        }

        Box(
            modifier         = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.Settings,
                contentDescription = "Configuracion",
                tint               = Color(0xFF9CA3AF),
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Mic waveform bars ────────────────────────────────────────────────────────

@Composable
private fun WaveformBars() {
    val transition = rememberInfiniteTransition(label = "waveform")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.height(20.dp)
    ) {
        repeat(5) { i ->
            val height by transition.animateFloat(
                initialValue  = 5f,
                targetValue   = 18f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(800, delayMillis = i * 120, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(Color.White, RoundedCornerShape(99.dp))
            )
        }
    }
}

// ─── Register face dialog ─────────────────────────────────────────────────────

@Composable
private fun RegisterFaceDialog(
    nombreInput  : String,
    onNameChange : (String) -> Unit,
    onConfirm    : () -> Unit,
    onDismiss    : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text("Registrar persona") },
        text   = {
            Column {
                Text("Ingresa el nombre para esta cara:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = nombreInput,
                    onValueChange = onNameChange,
                    label         = { Text("Nombre") },
                    singleLine    = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = nombreInput.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
