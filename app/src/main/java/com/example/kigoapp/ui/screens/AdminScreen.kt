package com.example.kigoapp.ui.screens

import android.content.res.Configuration
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.model.FaceUIState
import com.example.kigoapp.model.Persona
import com.example.kigoapp.ui.components.BackChip
import com.example.kigoapp.ui.components.admin.EmpresasTab
import com.example.kigoapp.ui.components.admin.PersonasTab
import com.example.kigoapp.ui.components.admin.TabButton
import com.example.kigoapp.ui.theme.KigoColors

private enum class AdminTab { PERSONAS, EMPRESAS }

@Composable
fun AdminScreen(
    personas      : List<Persona>,
    empresas      : List<Empresa>,
    faceUIState   : FaceUIState,
    onSetupCamera : (PreviewView) -> Unit,
    onAddPersona  : (Persona, List<List<Float>>) -> Unit,
    onAddEmpresa  : (Empresa) -> Unit,
    onBack        : () -> Unit
) {
    var tab by remember { mutableStateOf(AdminTab.PERSONAS) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KigoColors.AppBg)
            .statusBarsPadding()
            .padding(if (isLandscape) 12.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Administración", fontSize = if (isLandscape) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = KigoColors.TextPrimary)
                if (!isLandscape) Text("Alta de personas y empresas", fontSize = 13.sp, color = KigoColors.TextSecondary)
            }
            BackChip("Inicio", onBack)
        }

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

        // Portrait: outer scroll wraps stacked form+list. Landscape: tabs own their scroll.
        val contentMod = if (isLandscape)
            Modifier.fillMaxWidth().weight(1f)
        else
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())

        Column(modifier = contentMod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (tab) {
                AdminTab.PERSONAS -> PersonasTab(personas, empresas, faceUIState, onSetupCamera, onAddPersona, isLandscape)
                AdminTab.EMPRESAS -> EmpresasTab(empresas, onAddEmpresa, isLandscape)
            }
        }
    }
}
