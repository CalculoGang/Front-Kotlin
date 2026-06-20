package com.example.miprimeraapp

import android.content.Context
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.Persona
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AdminStorage {
    private const val ARCHIVO = "admin.json"

    fun guardar(context: Context, personas: List<Persona>, empresas: List<Empresa>) {
        val root = JSONObject()

        val perArr = JSONArray()
        personas.forEach { p ->
            perArr.put(
                JSONObject()
                    .put("nombre", p.nombre)
                    .put("tipo", p.tipo)
                    .put("empresa", p.empresa)
                    .put("empresa_origen", p.empresaOrigen)
                    .put("tipo_identificacion", p.tipoIdentificacion)
                    .put("telefono", p.telefono)
                    .put("correo", p.correo)
            )
        }
        root.put("personas", perArr)

        val empArr = JSONArray()
        empresas.forEach { e ->
            empArr.put(
                JSONObject()
                    .put("nombre", e.nombre)
                    .put("tipo", e.tipo)
                    .put("direccion", e.direccion)
                    .put("correo_contacto", e.correoContacto)
                    .put("telefono_contacto", e.telefonoContacto)
                    .put("activa", e.activa)
            )
        }
        root.put("empresas", empArr)

        File(context.filesDir, ARCHIVO).writeText(root.toString())
    }

    fun cargarPersonas(context: Context): List<Persona> {
        val root = leer(context) ?: return emptyList()
        val arr  = root.optJSONArray("personas") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Persona(
                nombre             = o.optString("nombre"),
                tipo               = o.optString("tipo", "visitante"),
                empresa            = o.optString("empresa"),
                empresaOrigen      = o.optString("empresa_origen"),
                tipoIdentificacion = o.optString("tipo_identificacion"),
                telefono           = o.optString("telefono"),
                correo             = o.optString("correo")
            )
        }
    }

    fun cargarEmpresas(context: Context): List<Empresa> {
        val root = leer(context) ?: return emptyList()
        val arr  = root.optJSONArray("empresas") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Empresa(
                nombre           = o.optString("nombre"),
                tipo             = o.optString("tipo"),
                direccion        = o.optString("direccion"),
                correoContacto   = o.optString("correo_contacto"),
                telefonoContacto = o.optString("telefono_contacto"),
                activa           = o.optBoolean("activa", true)
            )
        }
    }

    private fun leer(context: Context): JSONObject? {
        val archivo = File(context.filesDir, ARCHIVO)
        if (!archivo.exists()) return null
        return try {
            JSONObject(archivo.readText())
        } catch (e: Exception) {
            null
        }
    }
}
