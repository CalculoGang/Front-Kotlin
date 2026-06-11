package com.example.miprimeraapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.miprimeraapp.ui.components.BackChip
import com.example.miprimeraapp.ui.components.ModeCard
import com.example.miprimeraapp.ui.components.PermissionBullet
import com.example.miprimeraapp.ui.theme.KigoColors
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@Composable
fun ModeSelectScreen(
    onBack        : () -> Unit,
    onSelectTouch : () -> Unit,
    onSelectVoice : () -> Unit
) {
    val context = LocalContext.current
    var showAudioDialog by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onSelectVoice() }

    fun handleVoiceTap() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onSelectVoice() else showAudioDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            BackChip(label = "Volver", onClick = onBack)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Elige tu modo\nde registro", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = KigoColors.TextPrimary, lineHeight = 34.sp)
                Text("Selecciona la opción que prefieras.", fontSize = 14.sp, color = KigoColors.TextSecondary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModeCard(
                    badge       = "TÁCTIL",
                    badgeColor  = KigoColors.KigoRed,
                    title       = "Captura táctil",
                    desc        = "Escribe tu información directamente en pantalla.",
                    accentColor = KigoColors.KigoRed,
                    icon        = "✋",
                    onClick     = onSelectTouch
                )
                ModeCard(
                    badge       = "VOZ + IA",
                    badgeColor  = KigoColors.VoiceGreen,
                    title       = "Asistencia por voz",
                    desc        = "Un asistente con IA te guía paso a paso.",
                    accentColor = KigoColors.VoiceGreen,
                    icon        = "🎤",
                    onClick     = ::handleVoiceTap
                )
            }
        }
    }

    if (showAudioDialog) {
        AudioPermissionDialog(
            onAllow   = { showAudioDialog = false; audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDismiss = { showAudioDialog = false }
        )
    }
}

@Composable
private fun AudioPermissionDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Text("🎤", fontSize = 32.sp) },
        title = { Text("Permiso de micrófono", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("El asistente de voz necesita acceso al micrófono para:", fontSize = 14.sp, color = KigoColors.TextSecondary)
                PermissionBullet("Capturar tu voz y transcribirla con IA")
                PermissionBullet("Guiarte paso a paso en el registro")
                PermissionBullet("Extraer automáticamente tus datos")
                Text(
                    "Tu audio no se almacena ni se comparte con terceros.",
                    fontSize  = 12.sp,
                    color     = KigoColors.TextSecondary,
                    fontStyle = FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                colors  = ButtonDefaults.buttonColors(containerColor = KigoColors.VoiceGreen),
                shape   = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Text("Permitir micrófono", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = KigoColors.TextSecondary)
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    )
}
