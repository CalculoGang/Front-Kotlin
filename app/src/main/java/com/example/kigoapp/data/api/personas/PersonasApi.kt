package com.example.kigoapp.data.api.personas

import com.example.kigoapp.data.api.ApiClient
import com.example.kigoapp.model.Persona
import org.json.JSONArray
import org.json.JSONObject

object PersonasApi {

    fun listPersonas(): List<Persona> {
        val arr = JSONObject(ApiClient.get("/personas?limit=100")).optJSONArray("data") ?: JSONArray()
        return (0 until arr.length()).map { personaFrom(arr.getJSONObject(it)) }
    }

    /** empresaId obligatorio (FK). vector = 128 floats biométricos. */
    fun createPersona(p: Persona, empresaId: String, vector: List<Float>): Persona {
        val body = JSONObject()
            .put("empresa_id", empresaId)
            .put("nombre", p.nombre)
            .put("tipo", p.tipo)
            .put("empresa_origen", p.empresaOrigen)
            .put("tipo_identificacion", p.tipoIdentificacion)
            .put("identificacion_verificada", false)
            .put("vector_facial", JSONArray(vector))
            .put("version_modelo", ApiClient.VERSION_MODELO)
        return personaFrom(JSONObject(ApiClient.post("/personas", body)))
    }

    /**
     * POST /personas/buscar-rostro — identifica por vector facial.
     * Devuelve Persona reconocida o null si no hay match / error de red.
     * Backend exige exactamente 128 dims.
     */
    fun buscarPorRostro(vector: List<Float>): Persona? {
        if (vector.size != 128) {
            android.util.Log.w("PersonasApi", "vector_facial=${vector.size} dims; backend exige 128. Modelo .tflite no coincide.")
            return null
        }
        return try {
            val body = JSONObject().put("vector_facial", JSONArray(vector))
            val resp = JSONObject(ApiClient.post("/personas/buscar-rostro", body))
            if (resp.optBoolean("reconocido")) personaFrom(resp.getJSONObject("persona")) else null
        } catch (e: Exception) {
            // ponytail: sin distinguir 404 vs red caída — ambos = desconocido, es poll por-frame
            android.util.Log.w("PersonasApi", "buscarPorRostro: ${e.message}")
            null
        }
    }

    /** POST /personas/{id}/biometrico — añade muestra facial a galería 1→N. */
    fun agregarMuestra(personaId: String, vector: List<Float>) {
        if (vector.size != 128) {
            android.util.Log.w("PersonasApi", "agregarMuestra size=${vector.size}; backend exige 128")
            return
        }
        val body = JSONObject()
            .put("vector_facial", JSONArray(vector))
            .put("version_modelo", ApiClient.VERSION_MODELO)
        ApiClient.post("/personas/$personaId/biometrico", body)
    }

    private fun personaFrom(o: JSONObject) = Persona(
        id                 = o.optString("id"),
        nombre             = o.optString("nombre"),
        tipo               = o.optString("tipo", "visitante"),
        empresa            = o.optString("empresa_id"),
        empresaOrigen      = o.optString("empresa_origen"),
        tipoIdentificacion = o.optString("tipo_identificacion")
    )
}
