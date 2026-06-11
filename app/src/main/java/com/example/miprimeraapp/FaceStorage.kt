package com.example.miprimeraapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FaceStorage {
    private const val ARCHIVO = "faces.json"

    fun guardar(context: Context, personas: Map<String, List<List<Float>>>) {
        val json = JSONObject()
        for ((nombre, vectores) in personas) {
            val arr = JSONArray()
            for (v in vectores) {
                val vArr = JSONArray()
                v.forEach { vArr.put(it.toDouble()) }
                arr.put(vArr)
            }
            json.put(nombre, arr)
        }
        File(context.filesDir, ARCHIVO).writeText(json.toString())
    }

    fun cargar(context: Context): Map<String, MutableList<List<Float>>> {
        val archivo = File(context.filesDir, ARCHIVO)
        if (!archivo.exists()) return emptyMap()
        return try {
            val json = JSONObject(archivo.readText())
            val result = mutableMapOf<String, MutableList<List<Float>>>()
            for (nombre in json.keys()) {
                val arr = json.getJSONArray(nombre)
                val vectores = mutableListOf<List<Float>>()
                for (i in 0 until arr.length()) {
                    val v = arr.getJSONArray(i)
                    vectores.add(List(v.length()) { j -> v.getDouble(j).toFloat() })
                }
                result[nombre] = vectores
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
