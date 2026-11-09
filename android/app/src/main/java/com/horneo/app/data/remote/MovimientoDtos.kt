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

// Petición de entrada y de merma (MovimientoMateriaPrimaPeticion en el backend).
// La cantidad va siempre en positivo: en las mermas es el backend quien la convierte en negativa.
@Serializable
data class MovimientoMateriaPrimaPeticion(
    val materiaPrimaId: Long,
    @Serializable(with = BigDecimalSerializer::class) val cantidad: BigDecimal,
    // Opcional en las entradas y obligatorio en las mermas (lo comprueba el backend).
    val motivo: String?,
)

// Petición de ajuste (AjusteMateriaPrimaPeticion en el backend). No se envía la diferencia,
// sino el stock real que se ha contado: el backend calcula cuánto hay que sumar o restar.
@Serializable
data class AjusteMateriaPrimaPeticion(
    val materiaPrimaId: Long,
    @Serializable(with = BigDecimalSerializer::class) val stockReal: BigDecimal,
    val motivo: String,
)

// Peticiones de movimientos de productos (MovimientoProductoPeticion y AjusteProductoPeticion en el
// backend). Igual que las de materias primas, pero con cantidades enteras: los productos se cuentan
// por unidades. La entrada solo vale para productos de reventa (los elaborados suben con una producción).
@Serializable
data class MovimientoProductoPeticion(
    val productoId: Long,
    val cantidad: Int,
    val motivo: String?,
)

@Serializable
data class AjusteProductoPeticion(
    val productoId: Long,
    val stockReal: Int,
    val motivo: String,
)
