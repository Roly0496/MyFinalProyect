package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

// Mismos valores que el enum TipoMovimiento del backend.
@Serializable
enum class TipoMovimiento {
    ENTRADA,
    PRODUCCION,
    VENTA,
    MERMA,
    AJUSTE,
}

// Respuesta de los historiales de movimientos (MovimientoRespuesta en el backend).
// Un movimiento es de una materia prima o de un producto: el id del otro llega a null.
@Serializable
data class MovimientoRespuesta(
    val id: Long,
    @Serializable(with = LocalDateTimeSerializer::class) val fecha: LocalDateTime,
    val tipo: TipoMovimiento,
    val materiaPrimaId: Long?,
    val productoId: Long?,
    // Nombre de la materia prima o del producto.
    val elemento: String,
    // Con signo: positiva si entra stock y negativa si sale.
    @Serializable(with = BigDecimalSerializer::class) val cantidad: BigDecimal,
    // Solo en mermas y ajustes (en las mermas es obligatorio).
    val motivo: String?,
    // Nombre de quien hizo el movimiento.
    val usuario: String,
    // Solo si el movimiento viene de una producción o de una venta.
    val produccionId: Long?,
    val ventaId: Long?,
)
