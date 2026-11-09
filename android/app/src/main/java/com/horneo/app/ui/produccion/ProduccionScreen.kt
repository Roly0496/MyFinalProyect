package com.horneo.app.ui.produccion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.horneo.app.data.remote.ProduccionRespuesta
import com.horneo.app.ui.componentes.ContenidoLista
import com.horneo.app.ui.componentes.SelectorDia
import com.horneo.app.ui.formatearHora

// Pestaña Producción: lo que se ha fabricado en un día, con el selector de día
// y el botón para registrar una producción nueva.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduccionScreen(
    onRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProduccionViewModel = viewModel(factory = ProduccionViewModel.Factory),
) {
    val dia by viewModel.dia.collectAsStateWithLifecycle()
    val producciones by viewModel.producciones.collectAsStateWithLifecycle()

    // Se recarga cada vez que la pantalla vuelve a estar visible (por ejemplo, al registrar una producción).
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Producción") })

        SelectorDia(dia = dia, onCambiarDia = viewModel::elegirDia)

        // Box para poner el botón flotante encima de la lista, abajo a la derecha.
        Box(Modifier.fillMaxSize()) {
            ContenidoLista(
                estado = producciones,
                textoVacio = "No se ha producido nada este día",
                onReintentar = viewModel::cargar,
                clave = { it.id },
                // Hueco al final para que el botón flotante no tape la última fila.
                margenInferior = PaddingValues(bottom = 88.dp),
            ) { FilaProduccion(it) }

            ExtendedFloatingActionButton(
                onClick = onRegistrar,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text("Registrar producción") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun FilaProduccion(produccion: ProduccionRespuesta) {
    ListItem(
        headlineContent = { Text(produccion.producto) },
        supportingContent = { Text("${formatearHora(produccion.fecha)} · ${produccion.usuario}") },
        trailingContent = { Text("${produccion.cantidad} ud", style = MaterialTheme.typography.titleMedium) }
    )
}
