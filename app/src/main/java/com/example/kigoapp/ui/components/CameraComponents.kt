package com.example.kigoapp.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(onSetupCamera: (PreviewView) -> Unit) {
    val context     = LocalContext.current
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(Unit) { onSetupCamera(previewView) }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

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
