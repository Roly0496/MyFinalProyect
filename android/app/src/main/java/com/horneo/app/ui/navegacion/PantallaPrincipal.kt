package com.horneo.app.ui.navegacion

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.horneo.app.data.local.Sesion
import com.horneo.app.ui.almacen.AlmacenScreen
import com.horneo.app.ui.almacen.DetalleMateriaPrimaScreen
import com.horneo.app.ui.componentes.PantallaProvisional
import com.horneo.app.ui.inicio.InicioScreen

// Pantalla principal con sesión iniciada: barra inferior con las cuatro tareas del día a día
// y, en el centro, la pantalla de la pestaña activa.
@Composable
fun PantallaPrincipal(sesion: Sesion, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // Se vuelve a dibujar cada vez que cambia la pantalla actual, para marcar la pestaña activa.
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Pestana.entries.forEach { pestana ->
                    // hierarchy: la pestaña sigue marcada aunque se abra una pantalla "dentro" de ella.
                    val seleccionada = destinoActual?.hierarchy?.any { it.hasRoute(pestana.ruta::class) } == true
                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = {
                            navController.navigate(pestana.ruta) {
                                // Al cambiar de pestaña no se apilan pantallas: se vuelve a la de inicio
                                // y se guarda/restaura el estado de cada pestaña.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // Sin descripción: el texto de la pestaña ya la nombra (TalkBack no la lee dos veces).
                        icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                        label = { Text(pestana.etiqueta) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = InicioRuta,
            // consumeWindowInsets avisa a las pantallas de dentro de que el hueco de las barras del sistema
            // ya está cubierto, para que su TopAppBar no lo vuelva a dejar.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<InicioRuta> { InicioScreen(sesion) }
            navigation<AlmacenGrafo>(startDestination = AlmacenRuta) {
                composable<AlmacenRuta> {
                    AlmacenScreen(
                        onMateriaPrimaClick = { materiaPrima ->
                            navController.navigate(DetalleMateriaPrimaRuta(materiaPrima.id, materiaPrima.nombre))
                        }
                    )
                }
                composable<DetalleMateriaPrimaRuta> { entrada ->
                    // toRoute convierte la entrada de navegación en la ruta con sus datos (id y nombre).
                    val ruta = entrada.toRoute<DetalleMateriaPrimaRuta>()
                    DetalleMateriaPrimaScreen(
                        nombre = ruta.nombre,
                        onVolver = { navController.popBackStack() }
                    )
                }
            }
            composable<ProduccionRuta> { PantallaProvisional("Producción") }
            composable<VentasRuta> { PantallaProvisional("Ventas") }
        }
    }
}
