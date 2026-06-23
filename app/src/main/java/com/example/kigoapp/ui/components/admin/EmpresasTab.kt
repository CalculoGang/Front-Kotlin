package com.example.kigoapp.ui.components.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.ui.components.FormSectionTitle
import com.example.kigoapp.ui.components.KigoTextField
import com.example.kigoapp.ui.theme.KigoColors

@Composable
internal fun EmpresasTab(
    empresas   : List<Empresa>,
    onAdd      : (Empresa) -> Unit,
    isLandscape: Boolean = false
) {
    var nombre    by remember { mutableStateOf("") }
    var tipo      by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var correo    by remember { mutableStateOf("") }
    var telefono  by remember { mutableStateOf("") }
    var activa    by remember { mutableStateOf(true) }

    val formContent: @Composable ColumnScope.() -> Unit = {
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

    val listContent: @Composable ColumnScope.() -> Unit = {
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

    if (isLandscape) {
        Row(
            modifier            = Modifier.fillMaxSize(),
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
                    title     = "Empresas registradas",
                    empty     = empresas.isEmpty(),
                    emptyText = "Aún no hay empresas.",
                    content   = listContent
                )
            }
        }
    } else {
        FormCard(content = formContent)
        ListCard(
            title     = "Empresas registradas",
            empty     = empresas.isEmpty(),
            emptyText = "Aún no hay empresas.",
            content   = listContent
        )
    }
}
