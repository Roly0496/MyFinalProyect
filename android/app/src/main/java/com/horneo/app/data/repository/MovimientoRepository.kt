package com.horneo.app.data.repository

import com.horneo.app.data.remote.AjusteMateriaPrimaPeticion
import com.horneo.app.data.remote.AjusteProductoPeticion
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.MovimientoMateriaPrimaPeticion
import com.horneo.app.data.remote.MovimientoProductoPeticion
import com.horneo.app.data.remote.MovimientoRespuesta
import java.math.BigDecimal

// Registra movimientos. Igual que en el backend, el stock nunca se cambia directamente:
// siempre a través de un movimiento, que queda en el historial.
class MovimientoRepository(private val api: HorneoApi) {

    suspend fun registrarEntradaMateriaPrima(
        materiaPrimaId: Long,
        cantidad: BigDecimal,
        motivo: String?,
    ): Result<MovimientoRespuesta> = runCatching {
        api.entradaMateriaPrima(MovimientoMateriaPrimaPeticion(materiaPrimaId, cantidad, motivo))
    }

    // La cantidad perdida se envía en positivo; el backend la resta del stock.
    suspend fun registrarMermaMateriaPrima(
        materiaPrimaId: Long,
        cantidad: BigDecimal,
        motivo: String,
    ): Result<MovimientoRespuesta> = runCatching {
        api.mermaMateriaPrima(MovimientoMateriaPrimaPeticion(materiaPrimaId, cantidad, motivo))
    }

    suspend fun ajustarStockMateriaPrima(
        materiaPrimaId: Long,
        stockReal: BigDecimal,
        motivo: String,
    ): Result<MovimientoRespuesta> = runCatching {
        api.ajusteMateriaPrima(AjusteMateriaPrimaPeticion(materiaPrimaId, stockReal, motivo))
    }

    // Solo productos de reventa: los elaborados suben su stock con una producción.
    suspend fun registrarEntradaProducto(
        productoId: Long,
        cantidad: Int,
        motivo: String?,
    ): Result<MovimientoRespuesta> = runCatching {
        api.entradaProducto(MovimientoProductoPeticion(productoId, cantidad, motivo))
    }

    suspend fun registrarMermaProducto(
        productoId: Long,
        cantidad: Int,
        motivo: String,
    ): Result<MovimientoRespuesta> = runCatching {
        api.mermaProducto(MovimientoProductoPeticion(productoId, cantidad, motivo))
    }

    suspend fun ajustarStockProducto(
        productoId: Long,
        stockReal: Int,
        motivo: String,
    ): Result<MovimientoRespuesta> = runCatching {
        api.ajusteProducto(AjusteProductoPeticion(productoId, stockReal, motivo))
    }
}
