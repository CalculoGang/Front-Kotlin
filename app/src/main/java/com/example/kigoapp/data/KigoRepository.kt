package com.example.kigoapp.data

import com.example.kigoapp.data.api.empresas.EmpresasApi
import com.example.kigoapp.data.api.health.HealthApi
import com.example.kigoapp.data.api.personas.PersonasApi
import com.example.kigoapp.model.Empresa
import com.example.kigoapp.model.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KigoRepository {

    suspend fun health(): Boolean = withContext(Dispatchers.IO) { HealthApi.health() }

    suspend fun refrescar(): Pair<List<Persona>, List<Empresa>> = withContext(Dispatchers.IO) {
        PersonasApi.listPersonas() to EmpresasApi.listEmpresas()
    }

    suspend fun crearPersona(
        p: Persona,
        empresaId: String,
        muestras: List<List<Float>>
    ): List<Persona> = withContext(Dispatchers.IO) {
        val creada = PersonasApi.createPersona(p, empresaId, muestras.first())
        muestras.drop(1).forEach { PersonasApi.agregarMuestra(creada.id, it) }
        PersonasApi.listPersonas()
    }

    suspend fun crearEmpresa(e: Empresa): Empresa = withContext(Dispatchers.IO) {
        EmpresasApi.createEmpresa(e)
    }

    suspend fun guardarRostro(
        nombre: String,
        empresaId: String,
        vector: List<Float>
    ): List<Persona> = withContext(Dispatchers.IO) {
        PersonasApi.createPersona(Persona(nombre = nombre, tipo = "visitante", empresa = empresaId), empresaId, vector)
        PersonasApi.listPersonas()
    }

    suspend fun buscarPorRostro(vector: List<Float>): Persona? = withContext(Dispatchers.IO) {
        PersonasApi.buscarPorRostro(vector)
    }

    suspend fun agregarMuestra(personaId: String, vector: List<Float>) = withContext(Dispatchers.IO) {
        PersonasApi.agregarMuestra(personaId, vector)
    }
}
