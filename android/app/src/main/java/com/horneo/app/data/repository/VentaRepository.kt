package com.horneo.app.data.repository

import com.horneo.app.data.remote.CajaRespuesta
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.LineaVentaPeticion
import com.horneo.app.data.remote.RegistrarVentaPeticion
import com.horneo.app.data.remote.TicketRespuesta
import com.horneo.app.data.remote.VentaRespuesta
import java.time.LocalDate

class VentaRepository(private val api: HorneoApi) {

    // Ventas de un día, de la más reciente a la más antigua (el orden lo da el backend).
    suspend fun listarDelDia(dia: LocalDate): Result<List<VentaRespuesta>> =
        runCatching { api.ventas(dia.toString()) }

    suspend fun ticket(id: Long): Result<TicketRespuesta> = runCatching { api.ticket(id) }

    // Solo administrador: a un empleado el backend le responde 403.
    suspend fun caja(dia: LocalDate): Result<CajaRespuesta> = runCatching { api.caja(dia.toString()) }

    // "cantidades" relaciona cada producto con las unidades vendidas. El backend registra la venta
    // entera o nada: si falta stock de un solo producto, no se guarda ninguna línea.
    suspend fun registrar(cantidades: Map<Long, Int>): Result<TicketRespuesta> = runCatching {
        val lineas = cantidades.map { (productoId, cantidad) -> LineaVentaPeticion(productoId, cantidad) }
        api.registrarVenta(RegistrarVentaPeticion(lineas))
    }
}
