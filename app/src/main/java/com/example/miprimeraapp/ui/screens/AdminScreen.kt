package com.example.miprimeraapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.Persona
import com.example.miprimeraapp.ui.components.BackChip
import com.example.miprimeraapp.ui.components.FormSectionTitle
import com.example.miprimeraapp.ui.components.KigoDropdown
import com.example.miprimeraapp.ui.components.KigoTextField
import com.example.miprimeraapp.ui.theme.KigoColors

private enum class AdminTab { PERSONAS, EMPRESAS }

private val TIPOS_PERSONA = listOf("visitante", "proveedor", "empleado")
private val TIPOS_ID      = listOf("INE", "Pasaporte", "Licencia", "Credencial BUAP", "Otro")

@Composable
fun AdminScreen(
    personas     : List<Persona>,
    empresas     : List<Empresa>,
    onAddPersona : (Persona) -> Unit,
    onAddEmpresa : (Empresa) -> Unit,
    onBack       : () -> Unit
) {
    var tab by remember { mutableStateOf(AdminTab.PERSONAS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.AppBg)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Administración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KigoColors.TextPrimary)
                Text("Alta de personas y empresas", fontSize = 13.sp, color = KigoColors.TextSecondary)
            }
            BackChip("Inicio", onBack)
        }

        // Tab switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KigoColors.CardBg)
                .border(1.dp, KigoColors.Border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton("Personas (${personas.size})", tab == AdminTab.PERSONAS, Modifier.weight(1f)) {
                tab = AdminTab.PERSONAS
            }
            TabButton("Empresas (${empresas.size})", tab == AdminTab.EMPRESAS, Modifier.weight(1f)) {
                tab = AdminTab.EMPRESAS
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (tab) {
                AdminTab.PERSONAS -> PersonaSection(personas, empresas, onAddPersona)
                AdminTab.EMPRESAS -> EmpresaSection(empresas, onAddEmpresa)
            }
        }
    }
}

@Composable
private fun PersonaSection(
    personas : List<Persona>,
    empresas : List<Empresa>,
    onAdd    : (Persona) -> Unit
) {
    var nombre        by remember { mutableStateOf("") }
    var tipo          by remember { mutableStateOf("visitante") }
    var empresa       by remember { mutableStateOf("") }
    var empresaOrigen by remember { mutableStateOf("") }
    var tipoId        by remember { mutableStateOf("") }
    var telefono      by remember { mutableStateOf("") }
    var correo        by remember { mutableStateOf("") }

    val nombresEmpresas = empresas.map { it.nombre }
    val sinEmpresas     = nombresEmpresas.isEmpty()

    FormCard {
        FormSectionTitle("👤", "Nueva persona", KigoColors.VisitBlue)

        if (sinEmpresas) {
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

        SaveButton(enabled = nombre.isNotBlank() && empresa.isNotBlank()) {
            onAdd(
                Persona(
                    nombre             = nombre.trim(),
                    tipo               = tipo,
                    empresa            = empresa,
                    empresaOrigen      = empresaOrigen.trim(),
                    tipoIdentificacion = tipoId,
                    telefono           = telefono.trim(),
                    correo             = correo.trim()
                )
            )
            nombre = ""; tipo = "visitante"; empresa = ""
            empresaOrigen = ""; tipoId = ""; telefono = ""; correo = ""
        }
    }

    ListCard(
        title     = "Personas registradas",
        empty     = personas.isEmpty(),
        emptyText = "Aún no hay personas."
    ) {
        personas.forEach { p ->
            RecordRow(
                titulo  = p.nombre,
                detalle = listOfNotNull(
                    p.tipo.takeIf { it.isNotBlank() },
                    p.empresa.takeIf { it.isNotBlank() },
                    p.telefono.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            )
        }
    }
}

@Composable
private fun EmpresaSection(empresas: List<Empresa>, onAdd: (Empresa) -> Unit) {
    var nombre    by remember { mutableStateOf("") }
    var tipo      by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var correo    by remember { mutableStateOf("") }
    var telefono  by remember { mutableStateOf("") }
    var activa    by remember { mutableStateOf(true) }

    FormCard {
        FormSectionTitle("🏢", "Nueva empresa", KigoColors.IdOrange)
        KigoTextField("Nombre", nombre, "Razón social") { nombre = it }
        KigoTextField("Tipo", tipo, "Ej. Paquetería, Limpieza") { tipo = it }
        KigoTextField("Dirección", direccion, "Calle, número, colonia", multiline = true) { direccion = it }
        KigoTextField("Correo de contacto", correo, "contacto@empresa.com") { correo = it }
        KigoTextField("Teléfono de contacto", telefono, "10 dígitos") { telefono = it }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Activa", fontSize = 13.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Medium)
            Switch(
                checked         = activa,
                onCheckedChange = { activa = it },
                colors          = SwitchDefaults.colors(checkedTrackColor = KigoColors.VoiceGreen)
            )
        }

        SaveButton(enabled = nombre.isNotBlank()) {
            onAdd(
                Empresa(
                    nombre           = nombre.trim(),
                    tipo             = tipo.trim(),
                    direccion        = direccion.trim(),
                    correoContacto   = correo.trim(),
                    telefonoContacto = telefono.trim(),
                    activa           = activa
                )
            )
            nombre = ""; tipo = ""; direccion = ""; correo = ""; telefono = ""; activa = true
        }
    }

    ListCard(
        title     = "Empresas registradas",
        empty     = empresas.isEmpty(),
        emptyText = "Aún no hay empresas."
    ) {
        empresas.forEach { e ->
            RecordRow(
                titulo  = e.nombre + if (!e.activa) "  (inactiva)" else "",
                detalle = listOfNotNull(
                    e.tipo.takeIf { it.isNotBlank() },
                    e.telefonoContacto.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KigoColors.Surface)
            .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun ListCard(title: String, empty: Boolean, emptyText: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KigoColors.Surface)
            .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
        if (empty) {
            Text(emptyText, fontSize = 13.sp, color = KigoColors.Pending)
        } else {
            content()
        }
    }
}

@Composable
private fun RecordRow(titulo: String, detalle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KigoColors.CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = KigoColors.TextPrimary)
            if (detalle.isNotBlank()) {
                Text(detalle, fontSize = 12.sp, color = KigoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = KigoColors.KigoRed)
    ) {
        Text("Dar de alta", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) KigoColors.KigoRed else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (selected) Color.White else KigoColors.TextSecondary
        )
    }
}
