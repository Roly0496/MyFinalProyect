package com.horneo.app.ui.almacen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.TipoProducto
import com.horneo.app.ui.componentes.EtiquetaAviso
import com.horneo.app.ui.formatearCantidad
import com.horneo.app.ui.formatearPrecio

private val pestanas = listOf("Materias primas", "Productos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenScreen(
    // La pantalla no sabe adónde se navega: solo avisa de qué fila se ha tocado.
    // Quien la usa (PantallaPrincipal) decide qué hacer, así la pantalla no depende de la navegación.
    onMateriaPrimaClick: (MateriaPrimaRespuesta) -> Unit,
    onProductoClick: (ProductoRespuesta) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlmacenViewModel = viewModel(factory = AlmacenViewModel.Factory)
) {
    val materiasPrimas by viewModel.materiasPrimas.collectAsStateWithLifecycle()
    val productos by viewModel.productos.collectAsStateWithLifecycle()

    // Se ejecuta cada vez que la pantalla pasa a estar visible y activa (RESUMED): al abrirla
    // y también al volver del detalle, así el stock está al día después de un movimiento.
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }
    // Pestaña elegida: estado solo visual, se conserva al girar la pantalla.
    var pestanaElegida by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Almacén") })

        PrimaryTabRow(selectedTabIndex = pestanaElegida) {
            pestanas.forEachIndexed { indice, titulo ->
                Tab(
                    selected = pestanaElegida == indice,
                    onClick = { pestanaElegida = indice },
                    text = { Text(titulo) }
                )
            }
        }

        when (pestanaElegida) {
            0 -> ContenidoLista(
                estado = materiasPrimas,
                textoVacio = "Todavía no hay materias primas",
                onReintentar = viewModel::cargarMateriasPrimas,
                clave = { it.id }
            ) { materiaPrima -> FilaMateriaPrima(materiaPrima, onClick = { onMateriaPrimaClick(materiaPrima) }) }

            else -> ContenidoLista(
                estado = productos,
                textoVacio = "Todavía no hay productos",
                onReintentar = viewModel::cargarProductos,
                clave = { it.id }
            ) { producto -> FilaProducto(producto, onClick = { onProductoClick(producto) }) }
        }
    }
}

// Dibuja cualquier lista del almacén según su estado: cargando, error, vacía o con datos.
// Cada pestaña solo tiene que decir cómo se dibuja una fila (el parámetro fila).
@Composable
private fun <T> ContenidoLista(
    estado: ListaUiState<T>,
    textoVacio: String,
    onReintentar: () -> Unit,
    clave: (T) -> Any,
    fila: @Composable (T) -> Unit,
) {
    when (estado) {
        ListaUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ListaUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(estado.mensaje, textAlign = TextAlign.Center)
            Button(onClick = onReintentar) { Text("Reintentar") }
        }

        is ListaUiState.Listo ->
            if (estado.elementos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(textoVacio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // LazyColumn solo dibuja las filas visibles en pantalla (como un RecyclerView).
                LazyColumn(Modifier.fillMaxSize()) {
                    items(estado.elementos, key = clave) { elemento ->
                        fila(elemento)
                        HorizontalDivider()
                    }
                }
            }
    }
}

@Composable
private fun FilaMateriaPrima(materiaPrima: MateriaPrimaRespuesta, onClick: () -> Unit) {
    ListItem(
        // Toda la fila se puede tocar. onClickLabel es lo que TalkBack anuncia: «Doble toque para ver detalle».
        modifier = Modifier.clickable(onClickLabel = "Ver detalle", onClick = onClick),
        headlineContent = { Text(materiaPrima.nombre) },
        supportingContent = { Text("Mínimo ${formatearCantidad(materiaPrima.stockMinimo, materiaPrima.unidad)}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatearCantidad(materiaPrima.stockActual, materiaPrima.unidad),
                    style = MaterialTheme.typography.titleMedium
                )
                if (materiaPrima.bajoMinimo) {
                    EtiquetaAviso("Bajo mínimo")
                }
            }
        }
    )
}

// Según el boceto: tipo y precio de venta debajo del nombre, y el stock a la derecha.
// El precio de compra no se muestra aquí; el coste irá en el detalle del producto, solo para el admin.
@Composable
private fun FilaProducto(producto: ProductoRespuesta, onClick: () -> Unit) {
    val tipo = when (producto.tipo) {
        TipoProducto.ELABORADO -> "Elaborado"
        TipoProducto.REVENTA -> "Reventa"
    }
    ListItem(
        modifier = Modifier.clickable(onClickLabel = "Ver detalle", onClick = onClick),
        headlineContent = { Text(producto.nombre) },
        supportingContent = { Text("$tipo · ${formatearPrecio(producto.precioVenta)}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text("${producto.stockActual} ud", style = MaterialTheme.typography.titleMedium)
                if (producto.stockActual == 0) {
                    EtiquetaAviso("Agotado")
                }
            }
        }
    )
}
