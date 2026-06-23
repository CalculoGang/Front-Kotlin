package com.example.kigoapp.ui.components.admin

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.model.FaceUIState
import com.example.kigoapp.model.Persona
import com.example.kigoapp.ui.components.CameraPreview
import com.example.kigoapp.ui.components.FormSectionTitle
import com.example.kigoapp.ui.components.KigoDropdown
import com.example.kigoapp.ui.components.KigoTextField
import com.example.kigoapp.ui.theme.KigoColors

private val TIPOS_PERSONA = listOf("visitante", "proveedor", "empleado")
private val TIPOS_ID      = listOf("INE", "Pasaporte", "Licencia", "Credencial BUAP", "Otro")

@Composable
internal fun PersonasTab(
    personas      : List<Persona>,
    empresas      : List<Empresa>,
    faceUIState   : FaceUIState,
    onSetupCamera : (PreviewView) -> Unit,
    onAdd         : (Persona, List<List<Float>>) -> Unit,
    isLandscape   : Boolean = false
) {
    var nombre        by remember { mutableStateOf("") }
    var tipo          by remember { mutableStateOf("visitante") }
    var empresa       by remember { mutableStateOf("") }
    var empresaOrigen by remember { mutableStateOf("") }
    var tipoId        by remember { mutableStateOf("") }
    var telefono      by remember { mutableStateOf("") }
    var correo        by remember { mutableStateOf("") }
    var muestras      by remember { mutableStateOf<List<List<Float>>>(emptyList()) }

    val nombresEmpresas = empresas.map { it.nombre }

    val formContent: @Composable ColumnScope.() -> Unit = {
        FormSectionTitle("👤", "Nueva persona", KigoColors.VisitBlue)

        if (nombresEmpresas.isEmpty()) {
            Text(
                "Registra al menos una empresa antes de dar de alta personas (empresa es obligatoria).",
                fontSize = 12.sp,
                color    = KigoColors.KigoRed
            )
        }

        KigoTextField("Nombre", nombre, "Nombre completo") { nombre = it }
        KigoDropdown("Tipo", TIPOS_PERSONA, tipo) { tipo = it }
        KigoDropdown("Empresa", nombresEmpresas, empresa) { empresa = it }
        KigoTextField("Empresa de origen", empresaOrigen, "Empresa del visitante (opcional)") { empresaOrigen = it }
        KigoDropdown("Tipo de identificación", TIPOS_ID, tipoId) { tipoId = it }
        KigoTextField("Teléfono", telefono, "10 dígitos") { telefono = it }
        KigoTextField("Correo", correo, "correo@ejemplo.com") { correo = it }

        FaceCaptureBlock(
            faceUIState   = faceUIState,
            onSetupCamera = onSetupCamera,
            muestrasCount = muestras.size,
            isLandscape   = isLandscape,
            onCapture     = { if (faceUIState.vector.isNotEmpty()) muestras = muestras + listOf(faceUIState.vector) },
            onLimpiar     = { muestras = emptyList() }
        )

        SaveButton(enabled = nombre.isNotBlank() && empresa.isNotBlank() && muestras.isNotEmpty()) {
            onAdd(
                Persona(
                    nombre             = nombre.trim(),
                    tipo               = tipo,
                    empresa            = empresa,
                    empresaOrigen      = empresaOrigen.trim(),
                    tipoIdentificacion = tipoId,
                    telefono           = telefono.trim(),
                    correo             = correo.trim()
                ),
                muestras
            )
            nombre = ""; tipo = "visitante"; empresa = ""
            empresaOrigen = ""; tipoId = ""; telefono = ""; correo = ""
            muestras = emptyList()
        }
    }

    val listContent: @Composable ColumnScope.() -> Unit = {
        personas.forEach { p ->
            val empresaNombre = empresas.find { it.id == p.empresa }?.nombre ?: p.empresa
            RecordRow(
                titulo  = p.nombre,
                detalle = listOfNotNull(
                    p.tipo.takeIf { it.isNotBlank() },
                    empresaNombre.takeIf { it.isNotBlank() },
                    p.telefono.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier              = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier            = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormCard(content = formContent)
            }
            Column(
                modifier            = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ListCard(
                    title     = "Personas registradas",
                    empty     = personas.isEmpty(),
                    emptyText = "Aún no hay personas.",
                    content   = listContent
                )
            }
        }
    } else {
        FormCard(content = formContent)
        ListCard(
            title     = "Personas registradas",
            empty     = personas.isEmpty(),
            emptyText = "Aún no hay personas.",
            content   = listContent
        )
    }
}

@Composable
private fun FaceCaptureBlock(
    faceUIState   : FaceUIState,
    onSetupCamera : (PreviewView) -> Unit,
    muestrasCount : Int,
    isLandscape   : Boolean,
    onCapture     : () -> Unit,
    onLimpiar     : () -> Unit
) {
    FormSectionTitle("📷", "Rostro (biométrico)", KigoColors.VoiceGreen)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Portrait: tall 9:16. Landscape: wider 4:3 to avoid dominating half-pane.
            .aspectRatio(if (isLandscape) 4f / 3f else 9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF17172A))
    ) {
        CameraPreview(onSetupCamera = onSetupCamera)

        val (chipColor, chipText) = when {
            !faceUIState.hayRostro -> Color.Black.copy(alpha = 0.6f) to "Sin rostro"
            else                   -> KigoColors.VoiceGreen.copy(alpha = 0.85f) to "Rostro detectado"
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(chipColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(chipText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
    Text(
        if (muestrasCount > 0) "Muestras capturadas: $muestrasCount" else "Captura varias muestras (frontal, perfil, distinta luz).",
        fontSize = 12.sp,
        color    = if (muestrasCount > 0) KigoColors.VoiceGreen else KigoColors.TextSecondary
    )
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick  = onCapture,
            enabled  = faceUIState.hayRostro && faceUIState.vector.isNotEmpty(),
            modifier = Modifier.weight(1f).height(44.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.VoiceGreen)
        ) {
            Text(
                if (muestrasCount > 0) "+ Agregar muestra" else "Capturar rostro",
                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
        }
        if (muestrasCount > 0) {
            Button(
                onClick  = onLimpiar,
                modifier = Modifier.height(44.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.CardBg)
            ) {
                Text("Limpiar", color = KigoColors.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
