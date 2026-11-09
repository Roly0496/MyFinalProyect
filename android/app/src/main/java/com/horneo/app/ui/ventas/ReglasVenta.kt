package com.horneo.app.ui.ventas

import com.horneo.app.data.remote.ProductoRespuesta
import java.math.BigDecimal

// Reglas del carrito de una venta, en funciones normales de Kotlin para probarlas sin emulador.
// El carrito relaciona el id de cada producto con las unidades que se van a vender.
// Es un Map inmutable: cada función devuelve un carrito nuevo en lugar de modificar el anterior.
typealias Carrito = Map<Long, Int>

fun Carrito.cantidadDe(productoId: Long): Int = this[productoId] ?: 0

// No se puede vender más de lo que hay: el + se desactiva al llegar al stock.
fun Carrito.puedeSumar(producto: ProductoRespuesta): Boolean = cantidadDe(producto.id) < producto.stockActual

fun Carrito.sumar(producto: ProductoRespuesta): Carrito =
    if (puedeSumar(producto)) this + (producto.id to cantidadDe(producto.id) + 1) else this

// Al llegar a 0 el producto sale del carrito: así nunca se envía una línea con cantidad 0
// (el backend la rechazaría).
fun Carrito.restar(productoId: Long): Carrito {
    val cantidad = cantidadDe(productoId)
    return when {
        cantidad <= 0 -> this
        cantidad == 1 -> this - productoId
        else -> this + (productoId to cantidad - 1)
    }
}

// Número total de unidades ("6 artículos").
fun Carrito.articulos(): Int = values.sum()

// Total = Σ precio de venta × cantidad. Es orientativo: el total de verdad lo calcula el backend
// con los precios que tenga en el momento de cobrar, y es el que sale en el ticket.
fun Carrito.total(productos: List<ProductoRespuesta>): BigDecimal {
    val precios = productos.associate { it.id to it.precioVenta }
    return entries.fold(BigDecimal.ZERO) { suma, (productoId, cantidad) ->
        suma + (precios[productoId] ?: BigDecimal.ZERO).multiply(BigDecimal(cantidad))
    }
}

// Si el stock ha cambiado (por ejemplo, otra persona vendió a la vez), el carrito se recorta
// al stock nuevo y quita los productos que ya no existen o se han agotado.
fun Carrito.ajustarAlStock(productos: List<ProductoRespuesta>): Carrito {
    val stocks = productos.associate { it.id to it.stockActual }
    return mapValues { (productoId, cantidad) -> minOf(cantidad, stocks[productoId] ?: 0) }
        .filterValues { it > 0 }
}
