package com.example.kigoapp.data.api.empresas

import com.example.kigoapp.data.api.ApiClient
import com.example.kigoapp.model.Empresa
import org.json.JSONArray
import org.json.JSONObject

object EmpresasApi {

    fun listEmpresas(): List<Empresa> {
        val arr = JSONObject(ApiClient.get("/empresas?limit=100")).optJSONArray("data") ?: JSONArray()
        return (0 until arr.length()).map { empresaFrom(arr.getJSONObject(it)) }
    }

    fun createEmpresa(e: Empresa): Empresa {
        val body = JSONObject()
            .put("nombre", e.nombre)
            .put("tipo", e.tipo)
            .put("direccion", e.direccion)
            .put("correo_contacto", e.correoContacto)
            .put("telefono_contacto", e.telefonoContacto)
        return empresaFrom(JSONObject(ApiClient.post("/empresas", body)))
    }

    private fun empresaFrom(o: JSONObject) = Empresa(
        id               = o.optString("id"),
        nombre           = o.optString("nombre"),
        tipo             = o.optString("tipo"),
        direccion        = o.optString("direccion"),
        correoContacto   = o.optString("correo_contacto"),
        telefonoContacto = o.optString("telefono_contacto"),
        activa           = o.optBoolean("activa", true)
    )
}
