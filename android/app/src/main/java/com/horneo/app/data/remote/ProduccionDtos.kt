package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

// Respuesta de GET /api/producciones y de POST /api/producciones (ProduccionRespuesta en el backend).
// Una producción es la fabricación de una cantidad de un producto elaborado: gasta su receta
// multiplicada por la cantidad y suma esa cantidad al stock del producto.
@Serializable
data class ProduccionRespuesta(
    val id: Long,
    @Serializable(with = LocalDateTimeSerializer::class) val fecha: LocalDateTime,
    val productoId: Long,
    // Nombre del producto.
    val producto: String,
    // Unidades fabricadas (siempre enteras).
    val cantidad: Int,
    // Nombre de quien la registró.
    val usuario: String,
)

// Petición de POST /api/producciones (RegistrarProduccionPeticion en el backend).
@Serializable
data class RegistrarProduccionPeticion(
    val productoId: Long,
    val cantidad: Int,
)
