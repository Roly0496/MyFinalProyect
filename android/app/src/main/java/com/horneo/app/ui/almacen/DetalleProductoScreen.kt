package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.IngredienteRespuesta
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.TipoProducto
import com.horneo.app.data.remote.Unidad
import com.horneo.app.ui.componentes.EtiquetaAviso
import com.horneo.app.ui.formatearCantidad
import com.horneo.app.ui.formatearPrecio
import java.math.BigDecimal

// Detalle de un producto (boceto 06): tarjeta con precio y stock, coste y margen (solo administrador),
// receta por unidad (solo elaborados) e historial de movimientos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleProductoScreen(
    nombre: String,
    esAdmin: Boolean,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleProductoViewModel = viewModel(factory = DetalleProductoViewModel.factory(esAdmin)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Se recarga cada vez que la pantalla vuelve a estar visible, igual que el detalle de materia prima.
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(nombre) },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                }
            }
        )

        when (val estadoActual = estado) {
            DetalleProductoUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is DetalleProductoUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estadoActual.mensaje, textAlign = TextAlign.Center)
                Button(onClick = viewModel::cargar) { Text("Reintentar") }
            }

            is DetalleProductoUiState.Listo -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    TarjetaProducto(estadoActual.producto, estadoActual.coste, Modifier.padding(16.dp))
                }
                // Solo los elaborados tienen receta; los de reventa se compran ya hechos.
                if (estadoActual.producto.tipo == TipoProducto.ELABORADO) {
                    seccionReceta(estadoActual.receta)
                }
                // Los movimientos de productos siempre son en unidades.
                seccionHistorial(estadoActual.movimientos, Unidad.UNIDAD)
            }
        }
    }
}

@Composable
private fun TarjetaProducto(producto: ProductoRespuesta, coste: BigDecimal?, modifier: Modifier = Modifier) {
    val tipo = when (producto.tipo) {
        TipoProducto.ELABORADO -> "Elaborado"
        TipoProducto.REVENTA -> "Reventa"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tipo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Stock", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatearPrecio(producto.precioVenta), style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (producto.stockActual == 0) {
                        EtiquetaAviso("Agotado")
                    }
                    Text("${producto.stockActual} ud", style = MaterialTheme.typography.headlineMedium)
                }
            }
            // Coste y margen: solo llegan al administrador. El margen es lo que se gana con cada unidad.
            coste?.let {
                Text(
                    "Coste ${formatearPrecio(it)} · margen ${formatearPrecio(producto.precioVenta - it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Receta por unidad: cuánta materia prima hace falta para fabricar un producto.
private fun LazyListScope.seccionReceta(receta: List<IngredienteRespuesta>) {
    item {
        Text(
            "Receta (por unidad)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    if (receta.isEmpty()) {
        item {
            Text(
                "Este producto todavía no tiene receta",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    } else {
        items(receta, key = { it.id }) { ingrediente ->
            ListItem(
                headlineContent = { Text(ingrediente.materiaPrimaNombre) },
                trailingContent = {
                    Text(
                        formatearCantidad(ingrediente.cantidad, ingrediente.unidad),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
            HorizontalDivider()
        }
    }
}
