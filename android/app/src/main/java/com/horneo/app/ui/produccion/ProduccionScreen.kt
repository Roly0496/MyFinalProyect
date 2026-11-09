package com.horneo.app.ui.produccion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.horneo.app.ui.formatearDia
import com.horneo.app.ui.formatearHora
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Pestaña Producción: lo que se ha fabricado en un día, con flechas para cambiar de día,
// un calendario al tocar la fecha y el botón para registrar una producción nueva.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduccionScreen(
    onRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProduccionViewModel = viewModel(factory = ProduccionViewModel.Factory),
) {
    val dia by viewModel.dia.collectAsStateWithLifecycle()
    val producciones by viewModel.producciones.collectAsStateWithLifecycle()
    // Si el calendario está abierto. Estado solo visual, se conserva al girar la pantalla.
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    // Se recarga cada vez que la pantalla vuelve a estar visible (por ejemplo, al registrar una producción).
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Producción") })

        SelectorDia(
            dia = dia,
            onAnterior = viewModel::diaAnterior,
            onSiguiente = viewModel::diaSiguiente,
            onAbrirCalendario = { mostrarCalendario = true },
        )

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

    if (mostrarCalendario) {
        Calendario(
            dia = dia,
            onElegir = { elegido ->
                viewModel.elegirDia(elegido)
                mostrarCalendario = false
            },
            onCerrar = { mostrarCalendario = false },
        )
    }
}

// ‹ Hoy, 9 nov ›   La fecha del medio es un botón que abre el calendario.
@Composable
private fun SelectorDia(
    dia: LocalDate,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onAbrirCalendario: () -> Unit,
) {
    val hoy = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAnterior) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Día anterior")
        }
        TextButton(onClick = onAbrirCalendario, modifier = Modifier.weight(1f)) {
            Text(formatearDia(dia, hoy), style = MaterialTheme.typography.titleMedium)
        }
        // Hoy es el último día que se puede mirar: la flecha se desactiva (sigue visible para que
        // la fecha no se mueva de sitio).
        IconButton(onClick = onSiguiente, enabled = dia.isBefore(hoy)) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Día siguiente")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Calendario(dia: LocalDate, onElegir: (LocalDate) -> Unit, onCerrar: () -> Unit) {
    // El calendario de Material trabaja con milisegundos en UTC (la medianoche del día elegido),
    // así que se convierte de LocalDate a milisegundos y al revés siempre en UTC, para no
    // equivocarse de día por la diferencia horaria.
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            // Los días futuros no se pueden elegir.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !aLocalDate(utcTimeMillis).isAfter(LocalDate.now())
        }
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = { estado.selectedDateMillis?.let { onElegir(aLocalDate(it)) } ?: onCerrar() }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = estado)
    }
}

private fun aLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun FilaProduccion(produccion: ProduccionRespuesta) {
    ListItem(
        headlineContent = { Text(produccion.producto) },
        supportingContent = { Text("${formatearHora(produccion.fecha)} · ${produccion.usuario}") },
        trailingContent = { Text("${produccion.cantidad} ud", style = MaterialTheme.typography.titleMedium) }
    )
}
