package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProduccionDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun leeUnaProduccion() {
        // JSON con la misma forma que devuelve GET /api/producciones
        val produccion = json.decodeFromString<ProduccionRespuesta>(
            """{"id":12,"fecha":"2026-11-09T07:40:00","productoId":1,"producto":"Croissant","cantidad":40,"usuario":"Carmen"}"""
        )

        assertEquals("Croissant", produccion.producto)
        assertEquals(40, produccion.cantidad)
        assertEquals(LocalDateTime.of(2026, 11, 9, 7, 40), produccion.fecha)
    }

    @Test
    fun escribeLaPeticionConCantidadEntera() {
        assertEquals(
            """{"productoId":1,"cantidad":40}""",
            json.encodeToString(RegistrarProduccionPeticion(1, 40))
        )
    }

    @Test
    fun laFechaSeEnviaEnFormatoIso() {
        // Es lo que se pone en ?fecha=: el backend lo lee con @DateTimeFormat(iso = DATE)
        assertEquals("2026-11-09", LocalDate.of(2026, 11, 9).toString())
    }
}
