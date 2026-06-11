package com.example.miprimeraapp.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miprimeraapp.model.FaceUIState
import com.example.miprimeraapp.ui.components.*
import com.example.miprimeraapp.ui.theme.KigoColors

@Composable
fun VoiceScreen(
    faceUIState   : FaceUIState,
    onGuardarFace : (String, List<Float>) -> Unit,
    onSetupCamera : (PreviewView) -> Unit,
    onBack        : () -> Unit
) {
    var micActive      by remember { mutableStateOf(false) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nombreInput    by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(KigoColors.Surface)) {

        VoiceTopBar(onBack = onBack)

        CameraPanel(
            faceUIState   = faceUIState,
            onSetupCamera = onSetupCamera,
            onRegisterTap = {
                nombreInput = faceUIState.nombreReconocido ?: ""
                mostrarDialogo = true
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
        ) {
            AiDataPanel()
            HorizontalDivider(color = KigoColors.Border)
            AssistantChatPanel()
        }

        MicControls(micActive = micActive, onMicToggle = { micActive = !micActive })
    }

    if (mostrarDialogo) {
        RegisterFaceDialog(
            nombreInput    = nombreInput,
            onNameChange   = { nombreInput = it },
            onConfirm      = { onGuardarFace(nombreInput, faceUIState.vector); mostrarDialogo = false },
            onDismiss      = { mostrarDialogo = false }
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun VoiceTopBar(onBack: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().background(KigoColors.BgDark).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(KigoColors.VoiceGreen, CircleShape))
            Text("Kigo Kiosk", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepPips(total = 5, current = 1, accentColor = KigoColors.VoiceGreen)
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CameraPanel(
    faceUIState   : FaceUIState,
    onSetupCamera : (PreviewView) -> Unit,
    onRegisterTap : () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black)
    ) {
        CameraPreview(onSetupCamera = onSetupCamera)

        val (chipColor, chipText) = when {
            !faceUIState.hayRostro               -> Pair(Color.Black.copy(alpha = 0.6f), "Sin rostro")
            faceUIState.nombreReconocido != null -> Pair(KigoColors.VoiceGreen.copy(alpha = 0.85f), faceUIState.nombreReconocido!!)
            else                                 -> Pair(KigoColors.KigoRed.copy(alpha = 0.85f), "Desconocido")
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart).padding(10.dp)
                .background(chipColor, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = faceUIState.hayRostro, onClick = onRegisterTap)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(chipText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd).padding(10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).background(KigoColors.KigoRed, CircleShape))
                Text("CÁMARA EN VIVO", color = Color.White, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
        }

        CamCorner(Alignment.TopStart)
        CamCorner(Alignment.TopEnd)
        CamCorner(Alignment.BottomStart)
        CamCorner(Alignment.BottomEnd)
    }
}

@Composable
private fun AiDataPanel() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Datos extraídos por IA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextSecondary, letterSpacing = 0.5.sp)
        DataExtractRow(icon = "👤", field = "Nombre completo",   value = "Esperando...")
        DataExtractRow(icon = "🏢", field = "Empresa",           value = "Esperando...")
        DataExtractRow(icon = "📋", field = "Motivo de visita",  value = "Esperando...")
        DataExtractRow(icon = "📞", field = "Persona a visitar", value = "Esperando...")
        DataExtractRow(icon = "🪪", field = "Identificación",    value = "Esperando...")
    }
}

@Composable
private fun AssistantChatPanel() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Asistente de Registro", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(32.dp).background(KigoColors.VoiceGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = KigoColors.VoiceGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(KigoColors.CardBg, RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp))
                    .border(1.dp, KigoColors.Border, RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "¡Hola! Soy Kigo, tu asistente de registro. Presiona el micrófono y dime tu nombre completo para comenzar.",
                    fontSize = 13.sp, color = KigoColors.TextPrimary, lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun MicControls(micActive: Boolean, onMicToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KigoColors.CardBg)
            .border(1.dp, KigoColors.Border)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (micActive) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = 3.dp, height = (8 + it * 4).dp)
                            .background(KigoColors.VoiceGreen, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("Escuchando...", fontSize = 13.sp, color = KigoColors.VoiceGreen, fontWeight = FontWeight.Medium)
            } else {
                Text("Presiona el micrófono para ", fontSize = 13.sp, color = KigoColors.TextSecondary)
                Text("iniciar", fontSize = 13.sp, color = KigoColors.VoiceGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (micActive) KigoColors.VoiceGreen else KigoColors.KigoRed, CircleShape)
                .clickable(onClick = onMicToggle),
            contentAlignment = Alignment.Center
        ) {
            Text("🎤", fontSize = 26.sp)
        }

        Text("Toca para hablar · Kigo AI v2.4", fontSize = 11.sp, color = KigoColors.TextSecondary)
    }
}

@Composable
private fun RegisterFaceDialog(
    nombreInput  : String,
    onNameChange : (String) -> Unit,
    onConfirm    : () -> Unit,
    onDismiss    : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar persona") },
        text  = {
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
