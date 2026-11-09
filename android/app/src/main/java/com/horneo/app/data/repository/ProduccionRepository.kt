package com.horneo.app.data.repository

import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.ProduccionRespuesta
import com.horneo.app.data.remote.RegistrarProduccionPeticion
import java.time.LocalDate

class ProduccionRepository(private val api: HorneoApi) {

    // Producciones de un día, de la más reciente a la más antigua (el orden lo da el backend).
    // LocalDate.toString() da el formato que espera el backend (ISO 8601): "2026-11-09".
    suspend fun listarDelDia(dia: LocalDate): Result<List<ProduccionRespuesta>> =
        runCatching { api.producciones(dia.toString()) }

    // El backend descuenta la receta × cantidad de las materias primas y suma la cantidad al stock
    // del producto, todo o nada: si falta una sola materia prima, no se guarda nada.
    suspend fun registrar(productoId: Long, cantidad: Int): Result<ProduccionRespuesta> =
        runCatching { api.registrarProduccion(RegistrarProduccionPeticion(productoId, cantidad)) }
}
