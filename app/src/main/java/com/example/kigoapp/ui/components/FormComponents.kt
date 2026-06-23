package com.example.kigoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.ui.theme.KigoColors

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
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, KigoColors.Border, RoundedCornerShape(10.dp))
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
