package com.horneo.app.ui.ventas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.CajaRespuesta
import com.horneo.app.data.remote.VentaRespuesta
import com.horneo.app.ui.componentes.ContenidoLista
import com.horneo.app.ui.componentes.SelectorDia
import com.horneo.app.ui.formatearHora
import com.horneo.app.ui.formatearPrecio

// Pestaña Ventas: las ventas de un día, la caja (solo administrador) y el botón para vender.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    esAdmin: Boolean,
    onNuevaVenta: () -> Unit,
    onVenta: (VentaRespuesta) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VentasViewModel = viewModel(factory = VentasViewModel.factory(esAdmin)),
) {
    val dia by viewModel.dia.collectAsStateWithLifecycle()
    val ventas by viewModel.ventas.collectAsStateWithLifecycle()
    val caja by viewModel.caja.collectAsStateWithLifecycle()

    // Se recarga cada vez que la pantalla vuelve a estar visible (por ejemplo, después de cobrar).
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Ventas") })

        SelectorDia(dia = dia, onCambiarDia = viewModel::elegirDia)

        caja?.let { TarjetaCaja(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        Box(Modifier.fillMaxSize()) {
            ContenidoLista(
                estado = ventas,
                textoVacio = "No hay ventas este día",
                onReintentar = viewModel::cargar,
                clave = { it.id },
                margenInferior = PaddingValues(bottom = 88.dp),
            ) { venta -> FilaVenta(venta, onClick = { onVenta(venta) }) }

            ExtendedFloatingActionButton(
                onClick = onNuevaVenta,
                icon = { Icon(painterResource(R.drawable.ic_point_of_sale), contentDescription = null) },
                text = { Text("Nueva venta") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

// Caja del día, como en el boceto de Inicio: el total en grande y el número de ventas.
@Composable
private fun TarjetaCaja(caja: CajaRespuesta, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Caja del día",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(formatearPrecio(caja.total), style = MaterialTheme.typography.headlineMedium)
            Text(
                if (caja.numeroVentas == 1) "1 venta" else "${caja.numeroVentas} ventas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaVenta(venta: VentaRespuesta, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClickLabel = "Ver ticket", onClick = onClick),
        headlineContent = { Text("Venta #${venta.id}") },
        supportingContent = { Text("${formatearHora(venta.fecha)} · ${venta.usuario}") },
        trailingContent = { Text(formatearPrecio(venta.total), style = MaterialTheme.typography.titleMedium) }
    )
}
