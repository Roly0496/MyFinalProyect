package com.horneo.app.ui.componentes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.horneo.app.R
import com.horneo.app.ui.formatearDia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ‹ Hoy, 9 nov ›   Selector de día que comparten Producción y Ventas.
// Las flechas pasan al día anterior o siguiente y la fecha del medio abre un calendario.
// No deja ir al futuro: la flecha de la derecha se desactiva en hoy y el calendario bloquea esos días.
@Composable
fun SelectorDia(dia: LocalDate, onCambiarDia: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val hoy = LocalDate.now()
    // Si el calendario está abierto. Estado solo visual, se conserva al girar la pantalla.
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onCambiarDia(dia.minusDays(1)) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Día anterior")
        }
        TextButton(onClick = { mostrarCalendario = true }, modifier = Modifier.weight(1f)) {
            Text(formatearDia(dia, hoy), style = MaterialTheme.typography.titleMedium)
        }
        // Sigue visible aunque esté desactivada, para que la fecha no se mueva de sitio.
        IconButton(onClick = { onCambiarDia(dia.plusDays(1)) }, enabled = dia.isBefore(hoy)) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Día siguiente")
        }
    }

    if (mostrarCalendario) {
        Calendario(
            dia = dia,
            onElegir = { elegido ->
                onCambiarDia(elegido)
                mostrarCalendario = false
            },
            onCerrar = { mostrarCalendario = false },
        )
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
