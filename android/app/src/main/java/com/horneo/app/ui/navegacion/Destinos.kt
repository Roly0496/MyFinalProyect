package com.horneo.app.ui.navegacion

import androidx.annotation.DrawableRes
import com.horneo.app.R
import kotlinx.serialization.Serializable

// Rutas de navegación "con tipos" (type-safe): cada pantalla es un objeto en lugar de un texto,
// así el compilador avisa si una ruta está mal escrita. @Serializable es necesario porque
// Navigation Compose convierte la ruta en texto internamente.
@Serializable
data object InicioRuta

@Serializable
data object AlmacenRuta

@Serializable
data object ProduccionRuta

@Serializable
data object VentasRuta

// Pestañas de la barra inferior, en el orden en que aparecen.
enum class Pestana(val ruta: Any, val etiqueta: String, @DrawableRes val icono: Int) {
    INICIO(InicioRuta, "Inicio", R.drawable.ic_home),
    ALMACEN(AlmacenRuta, "Almacén", R.drawable.ic_inventory_2),
    PRODUCCION(ProduccionRuta, "Producción", R.drawable.ic_bakery_dining),
    VENTAS(VentasRuta, "Ventas", R.drawable.ic_point_of_sale),
}
