package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.ui.componentes.EtiquetaAviso
import com.horneo.app.ui.formatearCantidad
import com.horneo.app.ui.formatearPrecioUnitario

// Detalle de una materia prima (boceto 04): tarjeta con el stock y, debajo, su historial de movimientos.
// Los botones Entrada / Merma / Ajuste abren el formulario de movimientos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMateriaPrimaScreen(
    nombre: String,
    // El ajuste es solo para el administrador: para el empleado el botón no aparece.
    esAdmin: Boolean,
    onVolver: () -> Unit,
    onMovimiento: (TipoFormularioMovimiento) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleMateriaPrimaViewModel = viewModel(factory = DetalleMateriaPrimaViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Se ejecuta cada vez que la pantalla pasa a estar visible y activa (RESUMED): al abrirla
    // y también al cerrar el formulario, así se ve el movimiento que se acaba de guardar.
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(nombre) },
            navigationIcon = {
                // Es un botón, así que lleva descripción para TalkBack.
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                }
            }
        )

        when (val estadoActual = estado) {
            DetalleMateriaPrimaUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is DetalleMateriaPrimaUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estadoActual.mensaje, textAlign = TextAlign.Center)
                Button(onClick = viewModel::cargar) { Text("Reintentar") }
            }

            is DetalleMateriaPrimaUiState.Listo ->
                ContenidoDetalle(estadoActual.materiaPrima, estadoActual.movimientos, esAdmin, onMovimiento)
        }
    }
}

@Composable
private fun ContenidoDetalle(
    materiaPrima: MateriaPrimaRespuesta,
    movimientos: List<MovimientoRespuesta>,
    esAdmin: Boolean,
    onMovimiento: (TipoFormularioMovimiento) -> Unit,
) {
    // Una sola LazyColumn para todo: la tarjeta y el título van como elementos sueltos (item)
    // y el historial como lista (items). Así toda la pantalla se desplaza junta.
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TarjetaStock(materiaPrima, Modifier.padding(16.dp))
        }
        item {
            // En materias primas la entrada está siempre disponible; el ajuste, solo para el administrador.
            BotonesMovimiento(
                mostrarEntrada = true,
                mostrarAjuste = esAdmin,
                onMovimiento = onMovimiento,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        seccionHistorial(movimientos, materiaPrima.unidad)
    }
}

@Composable
private fun TarjetaStock(materiaPrima: MateriaPrimaRespuesta, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Stock actual",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    formatearCantidad(materiaPrima.stockActual, materiaPrima.unidad),
                    style = MaterialTheme.typography.headlineMedium
                )
                if (materiaPrima.bajoMinimo) {
                    EtiquetaAviso("Bajo mínimo")
                }
            }
            // El precio solo llega al administrador (para el empleado viene a null), así que solo se
            // muestra si existe. La pantalla no necesita saber el rol: la decisión ya la tomó el backend.
            val minimo = "Mínimo ${formatearCantidad(materiaPrima.stockMinimo, materiaPrima.unidad)}"
            val precio = materiaPrima.precioUnitario?.let { formatearPrecioUnitario(it, materiaPrima.unidad) }
            Text(
                if (precio != null) "$minimo · $precio" else minimo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
