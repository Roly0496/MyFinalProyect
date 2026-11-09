package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class MovimientoDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    // JSON con la misma forma que devuelve GET /api/materias-primas/{id}/movimientos
    private val merma = """
        {"id":40,"fecha":"2026-11-09T09:39:35.123456","tipo":"MERMA","materiaPrimaId":3,"productoId":null,
         "elemento":"Mantequilla","cantidad":-150.00,"motivo":"Caducada","usuario":"Carmen",
         "produccionId":null,"ventaId":null}
    """.trimIndent()

    @Test
    fun leeUnaMermaConSuFechaYCantidadNegativa() {
        val movimiento = json.decodeFromString<MovimientoRespuesta>(merma)

        assertEquals(TipoMovimiento.MERMA, movimiento.tipo)
        assertEquals(LocalDateTime.of(2026, 11, 9, 9, 39, 35, 123_456_000), movimiento.fecha)
        assertEquals("-150.00", movimiento.cantidad.toPlainString())
        assertEquals("Caducada", movimiento.motivo)
        assertNull(movimiento.productoId)
    }

    @Test
    fun leeUnaFechaSinFraccionesDeSegundo() {
        // Si los segundos son exactos, Spring Boot no escribe la parte decimal
        val fecha = json.decodeFromString(LocalDateTimeSerializer, "\"2026-11-09T07:40:00\"")

        assertEquals(LocalDateTime.of(2026, 11, 9, 7, 40), fecha)
    }
}
