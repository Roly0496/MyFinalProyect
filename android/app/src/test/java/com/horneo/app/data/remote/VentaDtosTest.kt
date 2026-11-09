package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class VentaDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun leeUnTicketConSusLineas() {
        // JSON con la misma forma que devuelve POST /api/ventas
        val ticket = json.decodeFromString<TicketRespuesta>(
            """
            {"venta":{"id":231,"fecha":"2026-11-09T10:42:00","total":8.20,"usuario":"Carmen"},
             "lineas":[
               {"productoId":1,"producto":"Croissant","cantidad":3,"precioUnitario":1.20,"subtotal":3.60},
               {"productoId":5,"producto":"Zumo de naranja","cantidad":1,"precioUnitario":1.80,"subtotal":1.80}
             ]}
            """.trimIndent()
        )

        assertEquals(231L, ticket.venta.id)
        assertEquals("8.20", ticket.venta.total.toPlainString())
        assertEquals(2, ticket.lineas.size)
        assertEquals("3.60", ticket.lineas[0].subtotal.toPlainString())
    }

    @Test
    fun leeLaCajaConSuFecha() {
        val caja = json.decodeFromString<CajaRespuesta>("""{"fecha":"2026-11-09","numeroVentas":57,"total":184.60}""")

        assertEquals(LocalDate.of(2026, 11, 9), caja.fecha)
        assertEquals(57, caja.numeroVentas)
        assertEquals("184.60", caja.total.toPlainString())
    }

    @Test
    fun laPeticionSoloLlevaProductosYCantidades() {
        val peticion = RegistrarVentaPeticion(listOf(LineaVentaPeticion(1, 3), LineaVentaPeticion(5, 1)))

        assertEquals(
            """{"lineas":[{"productoId":1,"cantidad":3},{"productoId":5,"cantidad":1}]}""",
            json.encodeToString(peticion)
        )
    }
}
