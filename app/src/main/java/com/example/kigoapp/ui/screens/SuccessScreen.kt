package com.example.kigoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.model.TouchFormData
import com.example.kigoapp.ui.components.ConfirmRow
import com.example.kigoapp.ui.theme.KigoColors

@Composable
fun SuccessScreen(formData: TouchFormData, onReset: () -> Unit) {
    val timeText = remember { currentTime() }

    Box(
        modifier         = Modifier.fillMaxSize().background(KigoColors.Surface).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier         = Modifier.size(72.dp).background(KigoColors.VoiceGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text("Acceso ",     fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KigoColors.TextPrimary)
                    Text("Autorizado", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KigoColors.VoiceGreen, fontStyle = FontStyle.Italic)
                }
                Text(
                    "Tu registro fue completado. Tu contacto ha sido notificado.",
                    fontSize = 14.sp, color = KigoColors.TextSecondary, textAlign = TextAlign.Center
                )
            }

            TicketCard(formData = formData, timeText = timeText)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = onReset,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
                ) {
                    Text("↺  Nuevo registro", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = onReset,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Inicio", color = KigoColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun TicketCard(formData: TouchFormData, timeText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().background(KigoColors.BgDark).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Kigo Self Check-in AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier.background(KigoColors.VoiceGreen, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("✓ Autorizado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier         = Modifier.fillMaxWidth().background(Color.White).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(KigoColors.CardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, KigoColors.Border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("QR", fontSize = 20.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
                Text("Muestra en entrada", fontSize = 11.sp, color = KigoColors.TextSecondary)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            ConfirmRow("Visitante",      formData.nombre.ifBlank   { "--" })
            ConfirmRow("Empresa",        formData.empresa.ifBlank  { "--" })
            ConfirmRow("Motivo",         formData.motivo.ifBlank   { "--" })
            ConfirmRow("Visita a",       formData.contacto.ifBlank { "--" })
            ConfirmRow("Hora de entrada",timeText, last = true)
        }

        Box(
            modifier         = Modifier.fillMaxWidth().background(KigoColors.CardBg).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Código válido por 24 horas · Presenta al personal de seguridad",
                fontSize = 11.sp, color = KigoColors.TextSecondary, textAlign = TextAlign.Center
            )
        }
    }
}
