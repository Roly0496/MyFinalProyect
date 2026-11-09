package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
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

    @Test
    fun escribeLaPeticionDeMermaComoLaEsperaElBackend() {
        val peticion = MovimientoMateriaPrimaPeticion(3, BigDecimal("150.5"), "Caducada")

        // Cantidad sin comillas y en positivo: el backend es quien la resta
        assertEquals(
            """{"materiaPrimaId":3,"cantidad":150.5,"motivo":"Caducada"}""",
            json.encodeToString(peticion)
        )
    }

    @Test
    fun unaEntradaSinMotivoLoEnviaANull() {
        val peticion = MovimientoMateriaPrimaPeticion(3, BigDecimal("1000"), null)

        assertEquals(
            """{"materiaPrimaId":3,"cantidad":1000,"motivo":null}""",
            json.encodeToString(peticion)
        )
    }

    @Test
    fun escribeLaPeticionDeAjusteConElStockReal() {
        val peticion = AjusteMateriaPrimaPeticion(3, BigDecimal("700.00"), "Recuento semanal")

        assertEquals(
            """{"materiaPrimaId":3,"stockReal":700.00,"motivo":"Recuento semanal"}""",
            json.encodeToString(peticion)
        )
    }

    @Test
    fun lasPeticionesDeProductosLlevanCantidadesEnteras() {
        // Sin decimales: el backend espera un Integer para los productos
        assertEquals(
            """{"productoId":5,"cantidad":12,"motivo":null}""",
            json.encodeToString(MovimientoProductoPeticion(5, 12, null))
        )
        assertEquals(
            """{"productoId":5,"stockReal":0,"motivo":"Recuento"}""",
            json.encodeToString(AjusteProductoPeticion(5, 0, "Recuento"))
        )
    }
}
