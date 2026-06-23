package com.example.kigoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kigoapp.ui.theme.KigoColors

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
