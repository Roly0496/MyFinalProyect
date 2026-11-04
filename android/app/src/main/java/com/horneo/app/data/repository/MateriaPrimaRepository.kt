package com.horneo.app.data.repository

import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.MateriaPrimaRespuesta

class MateriaPrimaRepository(private val api: HorneoApi) {

    // Materias primas activas, ordenadas por nombre (el orden lo da el backend).
    suspend fun listar(): Result<List<MateriaPrimaRespuesta>> = runCatching { api.materiasPrimas() }
}
