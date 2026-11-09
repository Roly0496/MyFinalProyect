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

// Un ingrediente de la receta (IngredienteRespuesta en el backend): cuánta materia prima hace falta
// para fabricar UNA unidad del producto. Trae la unidad para poder mostrar "50 g" sin otra petición.
@Serializable
data class IngredienteRespuesta(
    val id: Long,
    val materiaPrimaId: Long,
    val materiaPrimaNombre: String,
    val unidad: Unidad,
    @Serializable(with = BigDecimalSerializer::class) val cantidad: BigDecimal,
)

// Coste de una unidad del producto (solo administrador). En los elaborados lo calcula el backend con
// la receta y los precios de las materias primas; en los de reventa es su precio de compra.
@Serializable
data class CosteRespuesta(
    val productoId: Long,
    @Serializable(with = BigDecimalSerializer::class) val coste: BigDecimal,
)
