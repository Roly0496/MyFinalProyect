package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import java.math.BigDecimal

// Mismos valores que el enum TipoProducto del backend.
// ELABORADO: se fabrica en el obrador con su receta. REVENTA: se compra hecho y se vende tal cual.
@Serializable
enum class TipoProducto {
    ELABORADO,
    REVENTA,
}

// Respuesta de GET /api/productos (ProductoRespuesta en el backend).
@Serializable
data class ProductoRespuesta(
    val id: Long,
    val nombre: String,
    val tipo: TipoProducto,
    @Serializable(with = BigDecimalSerializer::class) val precioVenta: BigDecimal,
    // Solo los productos de reventa tienen precio de compra; en los elaborados llega null.
    @Serializable(with = BigDecimalSerializer::class) val precioCompra: BigDecimal?,
    // Los productos se cuentan por unidades enteras (INT en la base de datos).
    val stockActual: Int,
    val activo: Boolean,
)
