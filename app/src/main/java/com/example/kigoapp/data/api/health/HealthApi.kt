package com.example.kigoapp.data.api.health

import com.example.kigoapp.data.api.ApiClient
import java.net.HttpURLConnection
import java.net.URL

object HealthApi {
    /** GET /health — true si el backend responde 200. No lanza excepción. */
    fun health(): Boolean = try {
        val serverRoot = ApiClient.baseUrl.substringBefore("/api")
        val conn = (URL("$serverRoot/health").openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = 5000
            readTimeout    = 5000
        }
        try { conn.responseCode in 200..299 } finally { conn.disconnect() }
    } catch (e: Exception) { false }
}
