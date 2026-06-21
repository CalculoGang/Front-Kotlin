package com.example.miprimeraapp.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Pantalla de prueba aislada para VoiceAssistant. No está conectada a AppScreen ni KigoApp.
// Instanciar directamente o usar @Preview para validar el módulo de TTS.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen() {
    val context = LocalContext.current
    var asistente by remember { mutableStateOf<VoiceAssistant?>(null) }

    DisposableEffect(Unit) {
        val va = VoiceAssistant(context)
        asistente = va
        onDispose { va.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Prueba TTS — Kigo Voice") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedButton(
                onClick = { asistente?.detener() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Detener audio")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val mensajes = VoiceAssistant.CATALOGO.entries.sortedBy { it.key }
                items(mensajes) { (numero, texto) ->
                    MensajeRow(
                        numero = numero,
                        texto = texto,
                        onPlay = { asistente?.reproducirMensaje(numero) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MensajeRow(numero: Int, texto: String, onPlay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#$numero",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onPlay) {
                Text("▶")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 480, heightDp = 800)
@Composable
private fun VoiceTestScreenPreview() {
    VoiceTestScreen()
}
