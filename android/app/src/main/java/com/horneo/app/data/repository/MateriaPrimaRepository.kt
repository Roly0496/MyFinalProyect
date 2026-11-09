package com.horneo.app.data.repository

import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.MovimientoRespuesta

class MateriaPrimaRepository(private val api: HorneoApi) {

    // Materias primas activas, ordenadas por nombre (el orden lo da el backend).
    suspend fun listar(): Result<List<MateriaPrimaRespuesta>> = runCatching { api.materiasPrimas() }

    suspend fun obtener(id: Long): Result<MateriaPrimaRespuesta> = runCatching { api.materiaPrima(id) }

    // Historial de una materia prima, del más reciente al más antiguo (el orden lo da el backend).
    suspend fun movimientos(id: Long): Result<List<MovimientoRespuesta>> =
        runCatching { api.movimientosDeMateriaPrima(id) }
}
