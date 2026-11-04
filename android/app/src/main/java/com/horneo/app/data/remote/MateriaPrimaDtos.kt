package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import java.math.BigDecimal

// Mismos valores que el enum Unidad del backend. El símbolo es el que se muestra junto a las cantidades.
@Serializable
enum class Unidad(val simbolo: String) {
    GRAMO("g"),
    MILILITRO("ml"),
    UNIDAD("ud"),
}

// Respuesta de GET /api/materias-primas (MateriaPrimaRespuesta en el backend).
@Serializable
data class MateriaPrimaRespuesta(
    val id: Long,
    val nombre: String,
    val unidad: Unidad,
    @Serializable(with = BigDecimalSerializer::class) val stockActual: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val stockMinimo: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val precioUnitario: BigDecimal,
    val activo: Boolean,
    val bajoMinimo: Boolean,
)
