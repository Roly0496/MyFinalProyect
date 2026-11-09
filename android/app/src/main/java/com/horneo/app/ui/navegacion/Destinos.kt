package com.horneo.app.ui.navegacion

import androidx.annotation.DrawableRes
import com.horneo.app.R
import com.horneo.app.ui.almacen.ElementoMovimiento
import com.horneo.app.ui.almacen.TipoFormularioMovimiento
import kotlinx.serialization.Serializable

// Rutas de navegación "con tipos" (type-safe): cada pantalla es un objeto en lugar de un texto,
// así el compilador avisa si una ruta está mal escrita. @Serializable es necesario porque
// Navigation Compose convierte la ruta en texto internamente.
@Serializable
data object InicioRuta

// El Almacén es un grupo de pantallas (un subgrafo): la lista y los detalles que se abren desde ella.
// La pestaña apunta al grupo, así sigue marcada mientras se está en cualquiera de sus pantallas.
@Serializable
data object AlmacenGrafo

@Serializable
data object AlmacenRuta

// Ruta con datos: lleva el id para pedir la materia prima y el nombre para el título,
// que así se ve desde el primer momento, sin esperar a que lleguen los datos.
@Serializable
data class DetalleMateriaPrimaRuta(val id: Long, val nombre: String)

@Serializable
data class DetalleProductoRuta(val id: Long, val nombre: String)

// El mismo formulario sirve para entrada, merma y ajuste, de materias primas y de productos:
// el tipo y el elemento viajan en la ruta.
@Serializable
data class FormularioMovimientoRuta(
    val id: Long,
    val nombre: String,
    val tipo: TipoFormularioMovimiento,
    val elemento: ElementoMovimiento,
)

// Producción también es un subgrafo: la lista del día y el formulario para registrar.
@Serializable
data object ProduccionGrafo

@Serializable
data object ProduccionRuta

// productoId: si se abre desde el detalle de un producto, llega ya elegido. -1 significa "ninguno"
// (se usa -1 en lugar de null porque es más sencillo para la navegación con tipos).
@Serializable
data class RegistrarProduccionRuta(val productoId: Long = SIN_PRODUCTO)

const val SIN_PRODUCTO = -1L

// Ventas también es un subgrafo: la lista del día, la nueva venta y el ticket.
@Serializable
data object VentasGrafo

@Serializable
data object VentasRuta

@Serializable
data object NuevaVentaRuta

@Serializable
data class TicketRuta(val ventaId: Long)

// Pestañas de la barra inferior, en el orden en que aparecen.
enum class Pestana(val ruta: Any, val etiqueta: String, @DrawableRes val icono: Int) {
    INICIO(InicioRuta, "Inicio", R.drawable.ic_home),
    ALMACEN(AlmacenGrafo, "Almacén", R.drawable.ic_inventory_2),
    PRODUCCION(ProduccionGrafo, "Producción", R.drawable.ic_bakery_dining),
    VENTAS(VentasGrafo, "Ventas", R.drawable.ic_point_of_sale),
}
