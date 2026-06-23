package com.example.kigoapp.data

import com.example.kigoapp.KigoApi
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.model.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KigoRepository {

    suspend fun refrescar(): Pair<List<Persona>, List<Empresa>> = withContext(Dispatchers.IO) {
        KigoApi.listPersonas() to KigoApi.listEmpresas()
    }

    suspend fun crearPersona(
        p: Persona,
        empresaId: String,
        muestras: List<List<Float>>
    ): List<Persona> = withContext(Dispatchers.IO) {
        val creada = KigoApi.createPersona(p, empresaId, muestras.first())
        muestras.drop(1).forEach { KigoApi.agregarMuestra(creada.id, it) }
        KigoApi.listPersonas()
    }

    suspend fun crearEmpresa(e: Empresa): Empresa = withContext(Dispatchers.IO) {
        KigoApi.createEmpresa(e)
    }

    suspend fun guardarRostro(
        nombre: String,
        empresaId: String,
        vector: List<Float>
    ): List<Persona> = withContext(Dispatchers.IO) {
        KigoApi.createPersona(Persona(nombre = nombre, tipo = "visitante", empresa = empresaId), empresaId, vector)
        KigoApi.listPersonas()
    }

    suspend fun buscarPorRostro(vector: List<Float>): Persona? = withContext(Dispatchers.IO) {
        KigoApi.buscarPorRostro(vector)
    }

    suspend fun agregarMuestra(personaId: String, vector: List<Float>) = withContext(Dispatchers.IO) {
        KigoApi.agregarMuestra(personaId, vector)
    }
}
