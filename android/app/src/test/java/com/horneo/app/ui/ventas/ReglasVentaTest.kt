package com.horneo.app.ui.ventas

import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.TipoProducto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ReglasVentaTest {

    private fun producto(id: Long, precio: String, stock: Int) =
        ProductoRespuesta(id, "Producto $id", TipoProducto.ELABORADO, BigDecimal(precio), null, stock, true)

    private val croissant = producto(1, "1.20", 24)
    private val zumo = producto(5, "1.80", 2)
    private val ensaimada = producto(6, "1.60", 0)

    @Test
    fun sumarYRestarCambianLaCantidad() {
        val carrito = emptyMap<Long, Int>().sumar(croissant).sumar(croissant).restar(croissant.id)

        assertEquals(1, carrito.cantidadDe(croissant.id))
    }

    @Test
    fun noSePuedeVenderMasQueElStock() {
        val carrito = emptyMap<Long, Int>().sumar(zumo).sumar(zumo).sumar(zumo)

        assertEquals(2, carrito.cantidadDe(zumo.id))
        assertFalse(carrito.puedeSumar(zumo))
    }

    @Test
    fun unProductoAgotadoNoSePuedeAnadir() {
        assertFalse(emptyMap<Long, Int>().puedeSumar(ensaimada))
        assertTrue(emptyMap<Long, Int>().sumar(ensaimada).isEmpty())
    }

    @Test
    fun alLlegarACeroElProductoSaleDelCarrito() {
        val carrito = mapOf(croissant.id to 1).restar(croissant.id)

        // Así nunca se envía una línea con cantidad 0
        assertTrue(carrito.isEmpty())
        // Restar de un producto que no está no hace nada
        assertTrue(carrito.restar(croissant.id).isEmpty())
    }

    @Test
    fun calculaElTotalYLosArticulos() {
        // 3 croissants a 1,20 € + 1 zumo a 1,80 € = 5,40 €
        val carrito = mapOf(croissant.id to 3, zumo.id to 1)

        assertEquals(BigDecimal("5.40"), carrito.total(listOf(croissant, zumo)))
        assertEquals(4, carrito.articulos())
    }

    @Test
    fun seAjustaAlStockNuevo() {
        // Mientras se preparaba la venta, alguien vendió zumos y ya solo queda 1
        val carrito = mapOf(croissant.id to 3, zumo.id to 2)
        val stocksNuevos = listOf(croissant, producto(5, "1.80", 1))

        assertEquals(mapOf(croissant.id to 3, zumo.id to 1), carrito.ajustarAlStock(stocksNuevos))
    }

    @Test
    fun alAjustarSeQuitanLosAgotados() {
        val carrito = mapOf(croissant.id to 3, zumo.id to 2)
        val stocksNuevos = listOf(croissant, producto(5, "1.80", 0))

        assertEquals(mapOf(croissant.id to 3), carrito.ajustarAlStock(stocksNuevos))
    }
}
