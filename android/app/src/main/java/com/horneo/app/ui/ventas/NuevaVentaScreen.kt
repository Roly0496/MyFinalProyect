package com.horneo.app.ui.ventas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.ui.componentes.ContenidoLista
import com.horneo.app.ui.componentes.ListaUiState
import com.horneo.app.ui.formatearPrecio

// Nueva venta (boceto 08): pensada para usarla de pie en el mostrador, con botones grandes de + y −.
// No deja pasar del stock y el total se va actualizando. Al cobrar se abre el ticket.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    onCerrar: () -> Unit,
    onCobrado: (ventaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NuevaVentaViewModel = viewModel(factory = NuevaVentaViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.ventaRegistrada) {
        estado.ventaRegistrada?.let(onCobrado)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Nueva venta") },
            navigationIcon = {
                IconButton(onClick = onCerrar) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Cerrar sin cobrar")
                }
            }
        )

        ContenidoLista(
            estado = estado.productos,
            textoVacio = "Todavía no hay productos",
            onReintentar = viewModel::cargarProductos,
            clave = { it.id },
            // weight(1f): la lista ocupa todo el espacio que deja libre la barra del total.
            modifier = Modifier.weight(1f),
        ) { producto ->
            FilaProductoVenta(
                producto = producto,
                cantidad = estado.carrito.cantidadDe(producto.id),
                puedeSumar = estado.carrito.puedeSumar(producto),
                onSumar = { viewModel.sumar(producto) },
                onRestar = { viewModel.restar(producto) },
            )
        }

        // La barra del total solo tiene sentido cuando ya se ven los productos.
        val productos = (estado.productos as? ListaUiState.Listo)?.elementos
        if (productos != null) {
            BarraTotal(
                estado = estado,
                productos = productos,
                onCobrar = viewModel::cobrar,
            )
        }
    }
}

@Composable
private fun FilaProductoVenta(
    producto: ProductoRespuesta,
    cantidad: Int,
    puedeSumar: Boolean,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
) {
    val agotado = producto.stockActual == 0
    ListItem(
        // Los agotados se ven atenuados y no se pueden añadir.
        modifier = if (agotado) Modifier.alpha(0.5f) else Modifier,
        headlineContent = { Text(producto.nombre) },
        supportingContent = {
            Text(
                if (agotado) "${formatearPrecio(producto.precioVenta)} · agotado"
                else "${formatearPrecio(producto.precioVenta)} · quedan ${producto.stockActual}"
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botones de 48 dp (el mínimo recomendado para tocar con el dedo) y con descripción
                // para TalkBack que dice qué producto cambian.
                OutlinedIconButton(onClick = onRestar, enabled = cantidad > 0, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_remove), contentDescription = "Quitar un ${producto.nombre}")
                }
                Text(
                    "$cantidad",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 40.dp)
                )
                OutlinedIconButton(onClick = onSumar, enabled = puedeSumar, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = "Añadir un ${producto.nombre}")
                }
            }
        }
    )
}

// Abajo del todo: artículos, total y el botón Cobrar.
@Composable
private fun BarraTotal(
    estado: NuevaVentaUiState,
    productos: List<ProductoRespuesta>,
    onCobrar: () -> Unit,
) {
    val articulos = estado.carrito.articulos()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.errorEnvio?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (articulos == 1) "1 artículo" else "$articulos artículos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(formatearPrecio(estado.carrito.total(productos)), style = MaterialTheme.typography.headlineMedium)
                }
                Button(onClick = onCobrar, enabled = articulos > 0 && !estado.enviando) {
                    if (estado.enviando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cobrar")
                    }
                }
            }
        }
    }
}
