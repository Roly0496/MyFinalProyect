package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// Una venta, sin sus líneas (VentaRespuesta en el backend). Es lo que llega en la lista del día.
@Serializable
data class VentaRespuesta(
    val id: Long,
    @Serializable(with = LocalDateTimeSerializer::class) val fecha: LocalDateTime,
    @Serializable(with = BigDecimalSerializer::class) val total: BigDecimal,
    // Nombre de quien la cobró.
    val usuario: String,
)

// Una línea del ticket (LineaVentaRespuesta en el backend). El precio es el que tenía el producto
// en el momento de la venta: si después cambia el precio, el ticket no cambia.
@Serializable
data class LineaVentaRespuesta(
    val productoId: Long,
    val producto: String,
    val cantidad: Int,
    @Serializable(with = BigDecimalSerializer::class) val precioUnitario: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val subtotal: BigDecimal,
)

// El ticket: la venta y sus líneas (TicketRespuesta en el backend).
// Llega al cobrar (POST /api/ventas) y al abrir una venta de la lista (GET /api/ventas/{id}).
@Serializable
data class TicketRespuesta(
    val venta: VentaRespuesta,
    val lineas: List<LineaVentaRespuesta>,
)

// Caja de un día (CajaRespuesta en el backend). Solo administrador.
@Serializable
data class CajaRespuesta(
    @Serializable(with = LocalDateSerializer::class) val fecha: LocalDate,
    val numeroVentas: Int,
    @Serializable(with = BigDecimalSerializer::class) val total: BigDecimal,
)

// Petición de POST /api/ventas (RegistrarVentaPeticion y LineaVentaPeticion en el backend).
// Solo se envían productos y cantidades: el precio y el total los pone el backend.
@Serializable
data class LineaVentaPeticion(
    val productoId: Long,
    val cantidad: Int,
)

@Serializable
data class RegistrarVentaPeticion(
    val lineas: List<LineaVentaPeticion>,
)
