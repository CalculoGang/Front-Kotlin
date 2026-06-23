package com.example.kigoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.ui.theme.KigoColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WelcomeScreen(onStart: () -> Unit, onTestSpeech: () -> Unit = {}, onAdmin: () -> Unit = {}) {
    var timeText by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); timeText = currentTime() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KigoColors.BgDark, KigoColors.BgMid)))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            KigoLogo()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Bienvenido al", color = Color.White.copy(alpha = 0.85f), fontSize = 26.sp, textAlign = TextAlign.Center)
                Text("Self Check-in", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Registro de proveedores rápido, seguro y sin filas.",
                    color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp, textAlign = TextAlign.Center
                )
            }

            LiveClockPill(timeText)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
                ) {
                    Text("Iniciar Registro", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text("Tengo cita previa", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
                OutlinedButton(
                    onClick  = onAdmin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text("⚙ Administrador", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
            }

            androidx.compose.material3.TextButton(onClick = onTestSpeech) {
                Text("🎤 Probar reconocimiento de voz", color = Color.White.copy(alpha = 0.30f), fontSize = 11.sp)
            }

            Text("KIGO SELF CHECK-IN AI · v2.4", color = Color.White.copy(alpha = 0.25f), fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun KigoLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(KigoColors.KigoRed, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text("Kigo", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LiveClockPill(timeText: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).background(KigoColors.VoiceGreen, CircleShape))
        Text("$timeText · Sistema activo", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

internal fun currentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
