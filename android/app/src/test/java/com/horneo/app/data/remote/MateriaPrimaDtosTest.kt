package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MateriaPrimaDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    // JSON con la misma forma que devuelve GET /api/materias-primas
    private val respuesta = """
        {"id":3,"nombre":"Mantequilla","unidad":"GRAMO","stockActual":850.00,"stockMinimo":2000.00,
         "precioUnitario":0.01,"activo":true,"bajoMinimo":true}
    """.trimIndent()

    @Test
    fun leeLaMateriaPrimaDelJson() {
        val materiaPrima = json.decodeFromString<MateriaPrimaRespuesta>(respuesta)

        assertEquals("Mantequilla", materiaPrima.nombre)
        assertEquals(Unidad.GRAMO, materiaPrima.unidad)
        assertEquals("g", materiaPrima.unidad.simbolo)
        assertTrue(materiaPrima.bajoMinimo)
    }

    @Test
    fun conservaLosDecimalesExactos() {
        val materiaPrima = json.decodeFromString<MateriaPrimaRespuesta>(respuesta)

        // Se compara el texto para comprobar también la escala (850.00, no 850 ni 850.0)
        assertEquals("850.00", materiaPrima.stockActual.toPlainString())
        assertEquals("0.01", materiaPrima.precioUnitario?.toPlainString())
    }

    @Test
    fun leeLaRespuestaDelEmpleadoSinPrecio() {
        // Para el empleado, el backend manda el precio unitario a null
        val sinPrecio = respuesta.replace("\"precioUnitario\":0.01", "\"precioUnitario\":null")

        val materiaPrima = json.decodeFromString<MateriaPrimaRespuesta>(sinPrecio)

        assertNull(materiaPrima.precioUnitario)
        assertEquals("850.00", materiaPrima.stockActual.toPlainString())
    }

    @Test
    fun sumaSinErroresDeRedondeo() {
        // Con Double, 0.1 + 0.2 da 0.30000000000000004; con BigDecimal leído del JSON da 0.3 exacto
        val a = json.decodeFromString(BigDecimalSerializer, "0.1")
        val b = json.decodeFromString(BigDecimalSerializer, "0.2")

        assertEquals(BigDecimal("0.3"), a + b)
    }

    @Test
    fun escribeElNumeroSinComillas() {
        assertEquals("12.50", json.encodeToString(BigDecimalSerializer, BigDecimal("12.50")))
    }
}
