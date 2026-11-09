package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.ui.componentes.PantallaProvisional
import com.horneo.app.ui.formatearCantidad

private val pestanas = listOf("Materias primas", "Productos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenScreen(
    modifier: Modifier = Modifier,
    viewModel: AlmacenViewModel = viewModel(factory = AlmacenViewModel.Factory)
) {
    val estado by viewModel.materiasPrimas.collectAsStateWithLifecycle()
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
            0 -> ListaMateriasPrimas(estado, onReintentar = viewModel::cargar)
            else -> PantallaProvisional("Productos")
        }
    }
}

@Composable
private fun ListaMateriasPrimas(estado: MateriasPrimasUiState, onReintentar: () -> Unit) {
    when (estado) {
        MateriasPrimasUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is MateriasPrimasUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(estado.mensaje, textAlign = TextAlign.Center)
            Button(onClick = onReintentar) { Text("Reintentar") }
        }

        is MateriasPrimasUiState.Listo ->
            if (estado.materiasPrimas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Todavía no hay materias primas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // LazyColumn solo dibuja las filas visibles en pantalla (como un RecyclerView).
                LazyColumn(Modifier.fillMaxSize()) {
                    items(estado.materiasPrimas, key = { it.id }) { materiaPrima ->
                        FilaMateriaPrima(materiaPrima)
                        HorizontalDivider()
                    }
                }
            }
    }
}

@Composable
private fun FilaMateriaPrima(materiaPrima: MateriaPrimaRespuesta) {
    ListItem(
        headlineContent = { Text(materiaPrima.nombre) },
        supportingContent = { Text("Mínimo ${formatearCantidad(materiaPrima.stockMinimo, materiaPrima.unidad)}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatearCantidad(materiaPrima.stockActual, materiaPrima.unidad),
                    style = MaterialTheme.typography.titleMedium
                )
                if (materiaPrima.bajoMinimo) {
                    AvisoBajoMinimo()
                }
            }
        }
    )
}

// El aviso lleva texto además de color, para que se entienda aunque no se distinga el rojo.
@Composable
private fun AvisoBajoMinimo() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            "Bajo mínimo",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
