package com.example.kigoapp.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kigoapp.ui.theme.KigoColors

// ─── Camera ───────────────────────────────────────────────────────────────────

@Composable
fun CameraPreview(onSetupCamera: (PreviewView) -> Unit) {
    val context     = LocalContext.current
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(Unit) { onSetupCamera(previewView) }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

// ─── Navigation ───────────────────────────────────────────────────────────────

@Composable
fun BackChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(20.dp))
            .background(KigoColors.CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("←", color = KigoColors.TextSecondary, fontSize = 13.sp)
        Text(label,  color = KigoColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun StepPips(total: Int, current: Int, accentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i + 1 == current) 20.dp else 8.dp, height = 8.dp)
                    .background(
                        if (i + 1 <= current) accentColor else KigoColors.Border,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// ─── Form helpers ─────────────────────────────────────────────────────────────

@Composable
fun FormSectionTitle(icon: String, label: String, color: Color) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 14.sp)
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
    }
}

@Composable
fun KigoTextField(
    label      : String,
    value      : String,
    placeholder: String,
    multiline  : Boolean = false,
    onValue    : (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value         = value,
            onValueChange = onValue,
            placeholder   = { Text(placeholder, fontSize = 13.sp, color = KigoColors.Pending) },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = if (multiline) 3 else 1,
            maxLines      = if (multiline) 5 else 1,
            singleLine    = !multiline,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = KigoColors.Border,
                focusedBorderColor   = KigoColors.KigoRed
            )
        )
    }
}

@Composable
fun KigoDropdown(
    label   : String,
    options : List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text  = selected.ifBlank { "Selecciona..." },
                fontSize = 13.sp,
                color = if (selected.isBlank()) KigoColors.Pending else KigoColors.TextPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt, fontSize = 13.sp) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ConfirmRow(label: String, value: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = KigoColors.TextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KigoColors.TextPrimary)
        }
        if (!last) HorizontalDivider(color = KigoColors.Border, thickness = 0.5.dp)
    }
}

@Composable
fun PermissionBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Text("•", color = KigoColors.VoiceGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text, fontSize = 14.sp, color = KigoColors.TextPrimary)
    }
}

// ─── Camera frame corners ─────────────────────────────────────────────────────

@Composable
fun BoxScope.CamCorner(alignment: Alignment) {
    val shape = when (alignment) {
        Alignment.TopStart    -> RoundedCornerShape(topStart = 4.dp)
        Alignment.TopEnd      -> RoundedCornerShape(topEnd = 4.dp)
        Alignment.BottomStart -> RoundedCornerShape(bottomStart = 4.dp)
        else                  -> RoundedCornerShape(bottomEnd = 4.dp)
    }
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(8.dp)
            .size(18.dp)
            .border(2.dp, Color.White.copy(alpha = 0.6f), shape)
    )
}

// ─── Voice screen helpers ─────────────────────────────────────────────────────

@Composable
fun DataExtractRow(icon: String, field: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, KigoColors.Border, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(field, fontSize = 11.sp, color = KigoColors.TextSecondary)
            Text(value, fontSize = 12.sp, color = KigoColors.Pending)
        }
    }
}

// ─── Mode card ────────────────────────────────────────────────────────────────

@Composable
fun ModeCard(
    badge      : String,
    badgeColor : Color,
    title      : String,
    desc       : String,
    accentColor: Color,
    icon       : String,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KigoColors.Border, RoundedCornerShape(16.dp))
            .background(KigoColors.CardBg)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(icon, fontSize = 28.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = KigoColors.TextPrimary)
                Text(desc,  fontSize = 13.sp, color = KigoColors.TextSecondary)
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Comenzar", fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                Text("→", color = accentColor, fontSize = 13.sp)
            }
        }
    }
}
