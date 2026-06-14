package com.example.miprimeraapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miprimeraapp.ui.theme.KigoColors

// ─── Message model ────────────────────────────────────────────────────────────

sealed class ChatMessage {
    data class Kigo(val text: String) : ChatMessage()
    data class User(val text: String) : ChatMessage()
    data class ResidentFound(
        val name: String,
        val address: String,
        val isActive: Boolean = true
    ) : ChatMessage()
    object Typing : ChatMessage()
}

// ─── Conversation list ────────────────────────────────────────────────────────

@Composable
fun ConversationList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        itemsIndexed(messages) { index, msg ->
            val prev = messages.getOrNull(index - 1)
            when (msg) {
                is ChatMessage.Kigo -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (prev !is ChatMessage.Kigo) {
                            KigoSenderLabel()
                        }
                        KigoBubble(text = msg.text)
                    }
                }
                is ChatMessage.User -> UserBubble(text = msg.text)
                is ChatMessage.ResidentFound -> ResidentCardBubble(
                    name = msg.name,
                    address = msg.address,
                    isActive = msg.isActive
                )
                is ChatMessage.Typing -> TypingBubble()
            }
        }
    }
}

// ─── Sender label ─────────────────────────────────────────────────────────────

@Composable
fun KigoSenderLabel() {
    Text(
        text = "KIGO",
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = KigoColors.Pending,
        letterSpacing = 1.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

// ─── Kigo bubble (AI, left aligned) ──────────────────────────────────────────

@Composable
fun KigoBubble(text: String) {
    val shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(Color(0xFFEEEEF4), shape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF1C1C1E))
        }
    }
}

// ─── User bubble (right aligned) ─────────────────────────────────────────────

@Composable
fun UserBubble(text: String) {
    val shape = RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .background(KigoColors.KigoRed, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
        }
    }
}

// ─── Resident card bubble ─────────────────────────────────────────────────────

@Composable
fun ResidentCardBubble(name: String, address: String, isActive: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .shadow(1.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(Color(0xFFF5F5FA), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(1.dp, CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = KigoColors.Pending,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = address,
                    fontSize = 11.sp,
                    color = KigoColors.Pending,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (isActive) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFFECFDF5), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Text(
                        text = "Activo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

// ─── Typing bubble (animated dots) ───────────────────────────────────────────

@Composable
fun TypingBubble() {
    val shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.05f))
                .background(Color.White, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, delayMillis = i * 180),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFC7C7CC).copy(alpha = alpha), CircleShape)
                )
            }
            Text(
                text = "Esperando confirmacion...",
                fontSize = 12.sp,
                color = KigoColors.Pending
            )
        }
    }
}

// ─── Quick action chip ────────────────────────────────────────────────────────

@Composable
fun QuickActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(KigoColors.AppBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4B5563)
        )
    }
}
