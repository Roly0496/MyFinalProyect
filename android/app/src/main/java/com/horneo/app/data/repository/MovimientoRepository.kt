package com.horneo.app.data.repository

import com.horneo.app.data.remote.AjusteMateriaPrimaPeticion
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.MovimientoMateriaPrimaPeticion
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
}
