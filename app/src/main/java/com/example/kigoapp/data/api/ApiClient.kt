package com.example.kigoapp.data.api

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ponytail: HttpURLConnection + org.json — sin Retrofit/Ktor. La app ya usa org.json.
 * Todas las llamadas BLOQUEAN; invocar desde hilo de fondo.
 *
 * Emulador:          http://10.0.2.2:3000/api/v1
 * Mismo wifi que PC: http://192.168.101.23:3000/api/v1
 * Celular por cable (adb reverse tcp:3000 tcp:3000): http://127.0.0.1:3000/api/v1
 */
object ApiClient {
    var baseUrl = "http://127.0.0.1:3000/api/v1"

    // versión del modelo que guarda el backend junto al vector biométrico
    const val VERSION_MODELO = "mobilefacenet-128"

    /** GET /health — true si el backend responde 200. No lanza excepción. */
    fun health(): Boolean = try {
        val serverRoot = baseUrl.substringBefore("/api")
        val conn = (URL("$serverRoot/health").openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = 5000
            readTimeout    = 5000
        }
        try { conn.responseCode in 200..299 } finally { conn.disconnect() }
    } catch (e: Exception) { false }

    internal fun get(path: String) = request("GET", path, null)
    internal fun post(path: String, body: JSONObject) = request("POST", path, body)

    private fun request(method: String, path: String, body: JSONObject?): String {
        val conn = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
        }
        try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw RuntimeException("HTTP $code: $text")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
