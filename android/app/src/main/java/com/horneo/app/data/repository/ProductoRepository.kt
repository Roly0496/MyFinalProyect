package com.horneo.app.data.repository

import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.ProductoRespuesta

class ProductoRepository(private val api: HorneoApi) {

    // Productos activos de los dos tipos, ordenados por nombre (el orden lo da el backend).
    suspend fun listar(): Result<List<ProductoRespuesta>> = runCatching { api.productos() }
}
