package com.example.kigoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.model.TouchFormData
import com.example.kigoapp.ui.components.*
import com.example.kigoapp.ui.theme.KigoColors

@Composable
fun TouchFormScreen(
    formData : TouchFormData,
    onUpdate : (TouchFormData) -> Unit,
    onBack   : () -> Unit,
    onSubmit : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.Surface)
            .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackChip(label = "Modo", onClick = onBack)
                Column {
                    Text("Registro de Proveedor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
                    Text("Captura táctil · Kigo Self Check-in", fontSize = 11.sp, color = KigoColors.TextSecondary)
                }
            }
            StepPips(total = 4, current = formData.step, accentColor = KigoColors.KigoRed)
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
                .background(KigoColors.CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (formData.step) {
                    1 -> Step1Personal(formData, onUpdate)
                    2 -> Step2Visit(formData, onUpdate)
                    3 -> Step3Id(formData, onUpdate)
                    4 -> Step4Confirm(formData, onUpdate)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (formData.step > 1) {
                OutlinedButton(
                    onClick = { onUpdate(formData.copy(step = formData.step - 1)) },
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("← Anterior", color = KigoColors.TextPrimary) }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Button(
                onClick = {
                    if (formData.step < 4) onUpdate(formData.copy(step = formData.step + 1))
                    else onSubmit()
                },
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
            ) {
                Text(
                    if (formData.step < 4) "Siguiente →" else "Confirmar ✓",
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Step1Personal(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "👤", label = "Datos personales", color = KigoColors.KigoRed)
    KigoTextField("Nombre completo *",        data.nombre,   "Ej. Juan Carlos Martínez López") { onUpdate(data.copy(nombre   = it)) }
    KigoTextField("Empresa que representa *", data.empresa,  "Nombre de tu empresa")           { onUpdate(data.copy(empresa  = it)) }
    KigoTextField("Teléfono de contacto",     data.telefono, "55 1234 5678")                   { onUpdate(data.copy(telefono = it)) }
}

@Composable
private fun Step2Visit(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "📋", label = "Datos de la visita", color = KigoColors.VisitBlue)
    KigoTextField("Motivo de visita *",        data.motivo,   "Ej. Entrega de equipo, revisión de contrato...", multiline = true) { onUpdate(data.copy(motivo   = it)) }
    KigoTextField("Persona o área a visitar *",data.contacto, "Nombre o área")                                                   { onUpdate(data.copy(contacto = it)) }
    KigoDropdown(
        label    = "Tipo de visita",
        options  = listOf("Entrega de mercancía", "Servicio técnico", "Reunión de negocios", "Auditoría / revisión", "Otro"),
        selected = data.tipoVisita,
        onSelect = { onUpdate(data.copy(tipoVisita = it)) }
    )
}

@Composable
private fun Step3Id(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "🪪", label = "Identificación oficial", color = KigoColors.IdOrange)
    KigoDropdown(
        label    = "Tipo de identificación",
        options  = listOf("INE / IFE", "Pasaporte", "Licencia de conducir", "Credencial de empleado"),
        selected = data.tipoId,
        onSelect = { onUpdate(data.copy(tipoId = it)) }
    )
    IdCapturePlaceholder()
}

@Composable
private fun Step4Confirm(data: TouchFormData, onUpdate: (TouchFormData) -> Unit) {
    FormSectionTitle(icon = "✅", label = "Confirma tus datos", color = KigoColors.VoiceGreen)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        ConfirmRow("Nombre",        data.nombre.ifBlank   { "--" })
        ConfirmRow("Empresa",       data.empresa.ifBlank  { "--" })
        ConfirmRow("Motivo",        data.motivo.ifBlank   { "--" })
        ConfirmRow("Visita a",      data.contacto.ifBlank { "--" })
        ConfirmRow("Identificación",data.tipoId.ifBlank   { "--" }, last = true)
    }
    KigoTextField("Observaciones adicionales", data.observaciones, "Opcional...", multiline = true) {
        onUpdate(data.copy(observaciones = it))
    }
}

@Composable
private fun IdCapturePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📷", fontSize = 28.sp)
            Text("Toca para escanear tu ID",             color = Color.White,                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Acerca tu identificación a la cámara", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        CamCorner(Alignment.TopStart)
        CamCorner(Alignment.TopEnd)
        CamCorner(Alignment.BottomStart)
        CamCorner(Alignment.BottomEnd)
    }
}
