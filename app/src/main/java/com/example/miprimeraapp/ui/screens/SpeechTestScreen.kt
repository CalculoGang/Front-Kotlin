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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.miprimeraapp.ui.components.BackChip
import com.example.miprimeraapp.ui.theme.KigoColors
import java.util.Locale

@Composable
fun SpeechTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var isListening   by remember { mutableStateOf(false) }
    var finalText     by remember { mutableStateOf("") }
    var partialText   by remember { mutableStateOf("") }
    var statusMsg     by remember { mutableStateOf("Listo para escuchar") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) statusMsg = "Permiso denegado"
    }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusMsg   = "Escuchando..."
            }
            override fun onBeginningOfSpeech() { statusMsg = "Detectando voz..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                statusMsg   = "Procesando..."
            }
            override fun onError(error: Int) {
                isListening = false
                statusMsg   = "Error: ${speechErrorName(error)}"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                partialText = ""
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    finalText = matches[0]
                    statusMsg = "Listo · ${matches.size} variante(s) encontrada(s)"
                } else {
                    statusMsg = "Sin resultado"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) partialText = partial[0]
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    fun startListening() {
        if (!hasPermission) {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        finalText   = ""
        partialText = ""
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
        isListening = false
        statusMsg   = "Detenido manualmente"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KigoColors.BgDark, KigoColors.BgMid)))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BackChip(label = "Volver", onClick = onBack)

            Spacer(Modifier.height(20.dp))

            Text(
                "Test SpeechRecognizer",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Android STT · voz a texto nativo",
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp
            )

            Spacer(Modifier.weight(1f))

            // Status pill
            Row(
                modifier = Modifier
                    .background(
                        if (isListening) KigoColors.VoiceGreen.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        if (isListening) KigoColors.VoiceGreen.copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (isListening) KigoColors.VoiceGreen else KigoColors.Pending,
                            CircleShape
                        )
                )
                Text(statusMsg, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(36.dp))

            // Mic button with pulse
            Box(
                modifier = Modifier
                    .scale(if (isListening) pulseScale else 1f)
                    .size(120.dp)
                    .background(
                        if (isListening) KigoColors.VoiceGreen else KigoColors.KigoRed,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick  = { if (isListening) stopListening() else startListening() },
                    modifier = Modifier.fillMaxSize(),
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text     = if (isListening) "⏹" else "🎤",
                        fontSize = 38.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (isListening) "Toca para detener" else "Toca para hablar",
                color    = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp
            )

            Spacer(Modifier.weight(1f))

            // Transcription panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .heightIn(min = 130.dp, max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "TRANSCRIPCIÓN",
                    color         = Color.White.copy(alpha = 0.3f),
                    fontSize      = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight    = FontWeight.Medium
                )

                when {
                    finalText.isNotEmpty() -> Text(
                        finalText,
                        color      = Color.White,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    partialText.isNotEmpty() -> Text(
                        partialText,
                        color     = Color.White.copy(alpha = 0.5f),
                        fontSize  = 16.sp,
                        fontStyle = FontStyle.Italic
                    )
                    else -> Text(
                        "El texto reconocido aparecerá aquí...",
                        color     = Color.White.copy(alpha = 0.22f),
                        fontSize  = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            }

            if (finalText.isNotEmpty()) {
                TextButton(onClick = {
                    finalText   = ""
                    partialText = ""
                    statusMsg   = "Listo para escuchar"
                }) {
                    Text("Limpiar", color = KigoColors.TextSecondary, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

private fun speechErrorName(code: Int) = when (code) {
    SpeechRecognizer.ERROR_AUDIO                  -> "audio"
    SpeechRecognizer.ERROR_CLIENT                 -> "cliente"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "sin permisos"
    SpeechRecognizer.ERROR_NETWORK                -> "red"
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT        -> "timeout de red"
    SpeechRecognizer.ERROR_NO_MATCH               -> "sin coincidencia"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY        -> "reconocedor ocupado"
    SpeechRecognizer.ERROR_SERVER                 -> "servidor"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT         -> "timeout de voz"
    else                                          -> "desconocido ($code)"
}
