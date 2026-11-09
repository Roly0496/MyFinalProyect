package com.horneo.app.ui.navegacion

import androidx.annotation.DrawableRes
import com.horneo.app.R
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
data object ProduccionRuta

@Serializable
data object VentasRuta

// Pestañas de la barra inferior, en el orden en que aparecen.
enum class Pestana(val ruta: Any, val etiqueta: String, @DrawableRes val icono: Int) {
    INICIO(InicioRuta, "Inicio", R.drawable.ic_home),
    ALMACEN(AlmacenGrafo, "Almacén", R.drawable.ic_inventory_2),
    PRODUCCION(ProduccionRuta, "Producción", R.drawable.ic_bakery_dining),
    VENTAS(VentasRuta, "Ventas", R.drawable.ic_point_of_sale),
}
