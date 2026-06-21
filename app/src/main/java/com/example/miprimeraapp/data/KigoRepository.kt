package com.example.miprimeraapp.data

import android.content.Context
import com.example.miprimeraapp.AdminStorage
import com.example.miprimeraapp.KigoApi
import com.example.miprimeraapp.model.Empresa
import com.example.miprimeraapp.model.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Capa de datos: envuelve la red ([KigoApi]) y el cache local ([AdminStorage]).
 *
 * Todas las funciones son `suspend` y corren en [Dispatchers.IO]; reemplazan al
 * `netExecutor` + `runOnUiThread` que antes vivían en MainActivity. El cache local
 * se reescribe dentro del repo tras cada mutación, así el ViewModel no toca disco.
 */
class KigoRepository(private val appContext: Context) {

    /** Lee personas/empresas del cache JSON local (display instantáneo al arrancar). */
    suspend fun cargarCache(): Pair<List<Persona>, List<Empresa>> = withContext(Dispatchers.IO) {
        AdminStorage.cargarPersonas(appContext) to AdminStorage.cargarEmpresas(appContext)
    }

    /** Refresca desde el backend y persiste el resultado en el cache local. */
    suspend fun refrescar(): Pair<List<Persona>, List<Empresa>> = withContext(Dispatchers.IO) {
        val empresas = KigoApi.listEmpresas()
        val personas = KigoApi.listPersonas()
        AdminStorage.guardar(appContext, personas, empresas)
        personas to empresas
    }

    /**
     * Crea la persona con la 1ra muestra; las demás se añaden a su galería biométrica
     * (POST /personas/{id}/biometrico). Devuelve la lista actualizada y refresca el cache.
     */
    suspend fun crearPersona(
        p: Persona,
        empresaId: String,
        muestras: List<List<Float>>
    ): List<Persona> = withContext(Dispatchers.IO) {
        val creada = KigoApi.createPersona(p, empresaId, muestras.first())
        muestras.drop(1).forEach { KigoApi.agregarMuestra(creada.id, it) }
        listarYGuardar()
    }

    suspend fun crearEmpresa(e: Empresa): Empresa = withContext(Dispatchers.IO) {
        KigoApi.createEmpresa(e)
    }

    /** Registro rápido desde VoiceScreen: crea persona con un solo vector. */
    suspend fun guardarRostro(
        nombre: String,
        empresaId: String,
        vector: List<Float>
    ): List<Persona> = withContext(Dispatchers.IO) {
        KigoApi.createPersona(Persona(nombre = nombre, tipo = "visitante", empresa = empresaId), empresaId, vector)
        listarYGuardar()
    }

    suspend fun buscarPorRostro(vector: List<Float>): Persona? = withContext(Dispatchers.IO) {
        KigoApi.buscarPorRostro(vector)
    }

    suspend fun agregarMuestra(personaId: String, vector: List<Float>) = withContext(Dispatchers.IO) {
        KigoApi.agregarMuestra(personaId, vector)
    }

    // Re-lista personas tras una mutación y reescribe el cache. Empresas no cambian aquí:
    // se releen del cache local (sin golpear la red de nuevo).
    private fun listarYGuardar(): List<Persona> {
        val personas = KigoApi.listPersonas()
        AdminStorage.guardar(appContext, personas, AdminStorage.cargarEmpresas(appContext))
        return personas
    }
}
