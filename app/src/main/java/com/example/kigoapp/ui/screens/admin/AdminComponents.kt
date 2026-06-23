package com.example.kigoapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.ui.theme.KigoColors

@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
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
internal fun ListCard(
    title    : String,
    empty    : Boolean,
    emptyText: String,
    content  : @Composable ColumnScope.() -> Unit
) {
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
internal fun RecordRow(titulo: String, detalle: String) {
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
internal fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
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
internal fun TabButton(
    label   : String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
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
