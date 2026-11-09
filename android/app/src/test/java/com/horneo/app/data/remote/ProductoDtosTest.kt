package com.horneo.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductoDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    // JSON con la misma forma que devuelve GET /api/productos
    private val elaborado = """
        {"id":1,"nombre":"Croissant","tipo":"ELABORADO","precioVenta":1.20,"precioCompra":null,
         "stockActual":24,"activo":true}
    """.trimIndent()

    private val reventa = """
        {"id":5,"nombre":"Zumo de naranja","tipo":"REVENTA","precioVenta":1.80,"precioCompra":0.95,
         "stockActual":12,"activo":true}
    """.trimIndent()

    @Test
    fun leeUnProductoElaboradoSinPrecioDeCompra() {
        val producto = json.decodeFromString<ProductoRespuesta>(elaborado)

        assertEquals("Croissant", producto.nombre)
        assertEquals(TipoProducto.ELABORADO, producto.tipo)
        assertEquals("1.20", producto.precioVenta.toPlainString())
        assertNull(producto.precioCompra)
        assertEquals(24, producto.stockActual)
    }

    @Test
    fun leeUnProductoDeReventaConPrecioDeCompra() {
        val producto = json.decodeFromString<ProductoRespuesta>(reventa)

        assertEquals(TipoProducto.REVENTA, producto.tipo)
        assertEquals("0.95", producto.precioCompra?.toPlainString())
    }
}
