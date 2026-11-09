package com.horneo.app.ui.ventas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.TicketRespuesta
import com.horneo.app.ui.formatearFechaHora
import com.horneo.app.ui.formatearPrecio
import com.horneo.app.ui.theme.entrada

// Ticket de una venta (boceto 09). Si se acaba de cobrar, muestra "Venta registrada" y los botones
// para empezar otra venta o volver; si se abre desde la lista, solo el ticket.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    ventaId: Long,
    recienCobrada: Boolean,
    onCerrar: () -> Unit,
    onNuevaVenta: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TicketViewModel = viewModel(factory = TicketViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Venta #$ventaId") },
            navigationIcon = {
                IconButton(onClick = onCerrar) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Cerrar ticket")
                }
            }
        )

        when (val estadoActual = estado) {
            TicketUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is TicketUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estadoActual.mensaje, textAlign = TextAlign.Center)
                Button(onClick = viewModel::cargar) { Text("Reintentar") }
            }

            is TicketUiState.Listo -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (recienCobrada) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Decorativo: el texto de debajo ya dice lo que ha pasado.
                        Icon(
                            painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.entrada,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Venta registrada",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.entrada
                        )
                    }
                }

                TarjetaTicket(estadoActual.ticket)

                if (recienCobrada) {
                    Button(onClick = onNuevaVenta, modifier = Modifier.fillMaxWidth()) { Text("Nueva venta") }
                    OutlinedButton(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) { Text("Volver a ventas") }
                }
            }
        }
    }
}

// El ticket en sí: fecha y quién cobró, una línea por producto y el total.
// Los precios son los que guardó el backend al cobrar, no los actuales.
@Composable
private fun TarjetaTicket(ticket: TicketRespuesta) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatearFechaHora(ticket.venta.fecha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    ticket.venta.usuario,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            ticket.lineas.forEach { linea ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${linea.cantidad} × ${linea.producto}", modifier = Modifier.weight(1f))
                    Text(formatearPrecio(linea.subtotal))
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatearPrecio(ticket.venta.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
