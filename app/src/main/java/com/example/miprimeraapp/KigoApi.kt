package com.example.miprimeraapp

import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.Persona
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP del backend Go (Fiber, /api/v1).
 *
 * ponytail: HttpURLConnection + org.json — sin Retrofit/Ktor. La app ya usa org.json.
 * Todas las llamadas BLOQUEAN; invocar desde un hilo de fondo. Lanzan excepcion en error.
 */
object KigoApi {
    // Celular real por cable con `adb reverse tcp:3000 tcp:3000`: 127.0.0.1 del
    // telefono apunta al backend de la PC (como emulador). Re-correr adb reverse
    // tras reconectar/reiniciar el cel.
    //   - Emulador:            http://10.0.2.2:3000/api/v1
    //   - Misma wifi que PC:   http://192.168.101.23:3000/api/v1
    var baseUrl = "http://127.0.0.1:3000/api/v1"

    // version_modelo que guarda el backend junto al vector biometrico.
    const val VERSION_MODELO = "mobilefacenet-128"

    fun listEmpresas(): List<Empresa> {
        val arr = JSONObject(get("/empresas?limit=100")).optJSONArray("data") ?: JSONArray()
        return (0 until arr.length()).map { empresaFrom(arr.getJSONObject(it)) }
    }

    fun createEmpresa(e: Empresa): Empresa {
        val body = JSONObject()
            .put("nombre", e.nombre)
            .put("tipo", e.tipo)
            .put("direccion", e.direccion)
            .put("correo_contacto", e.correoContacto)
            .put("telefono_contacto", e.telefonoContacto)
        return empresaFrom(JSONObject(post("/empresas", body)))
    }

    fun listPersonas(): List<Persona> {
        val arr = JSONObject(get("/personas?limit=100")).optJSONArray("data") ?: JSONArray()
        return (0 until arr.length()).map { personaFrom(arr.getJSONObject(it)) }
    }

    /** empresaId obligatorio (FK). vector = 192 floats biometricos (puede ir vacio). */
    fun createPersona(p: Persona, empresaId: String, vector: List<Float>): Persona {
        val body = JSONObject()
            .put("empresa_id", empresaId)
            .put("nombre", p.nombre)
            .put("tipo", p.tipo)
            .put("empresa_origen", p.empresaOrigen)
            .put("tipo_identificacion", p.tipoIdentificacion)
            .put("identificacion_verificada", false)
            .put("vector_facial", JSONArray(vector))
            .put("version_modelo", VERSION_MODELO)
        return personaFrom(JSONObject(post("/personas", body)))
    }

    // ─── mapeo JSON → modelo ──────────────────────────────────────────────────
    private fun empresaFrom(o: JSONObject) = Empresa(
        id               = o.optString("id"),
        nombre           = o.optString("nombre"),
        tipo             = o.optString("tipo"),
        direccion        = o.optString("direccion"),
        correoContacto   = o.optString("correo_contacto"),
        telefonoContacto = o.optString("telefono_contacto"),
        activa           = o.optBoolean("activa", true)
    )

    private fun personaFrom(o: JSONObject) = Persona(
        id                 = o.optString("id"),
        nombre             = o.optString("nombre"),
        tipo               = o.optString("tipo", "visitante"),
        empresa            = o.optString("empresa_id"),   // del backend llega como empresa_id
        empresaOrigen      = o.optString("empresa_origen"),
        tipoIdentificacion = o.optString("tipo_identificacion")
    )

    // ─── transporte ───────────────────────────────────────────────────────────
    private fun get(path: String) = request("GET", path, null)
    private fun post(path: String, body: JSONObject) = request("POST", path, body)

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
