package com.horneo.app.data.repository

import com.horneo.app.data.remote.CosteRespuesta
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.IngredienteRespuesta
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.data.remote.ProductoRespuesta

class ProductoRepository(private val api: HorneoApi) {

    // Productos activos de los dos tipos, ordenados por nombre (el orden lo da el backend).
    suspend fun listar(): Result<List<ProductoRespuesta>> = runCatching { api.productos() }

    suspend fun obtener(id: Long): Result<ProductoRespuesta> = runCatching { api.producto(id) }

    // Receta por unidad. Los productos de reventa no tienen: la lista llega vacía.
    suspend fun receta(id: Long): Result<List<IngredienteRespuesta>> = runCatching { api.recetaDeProducto(id) }

    // Solo para el administrador: a un empleado el backend le responde 403.
    suspend fun coste(id: Long): Result<CosteRespuesta> = runCatching { api.costeDeProducto(id) }

    // Historial del producto, del más reciente al más antiguo (el orden lo da el backend).
    suspend fun movimientos(id: Long): Result<List<MovimientoRespuesta>> = runCatching { api.movimientosDeProducto(id) }
}
