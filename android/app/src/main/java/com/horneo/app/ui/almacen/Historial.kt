package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.data.remote.TipoMovimiento
import com.horneo.app.data.remote.Unidad
import com.horneo.app.ui.formatearCantidadConSigno
import com.horneo.app.ui.formatearFechaHora
import com.horneo.app.ui.theme.entrada

// Sección "Historial" que comparten el detalle de materia prima y el de producto.
// Es una función de extensión de LazyListScope: añade sus elementos dentro de la LazyColumn
// de cada pantalla, así toda la pantalla se sigue desplazando junta.
internal fun LazyListScope.seccionHistorial(movimientos: List<MovimientoRespuesta>, unidad: Unidad) {
    item {
        Text(
            "Historial",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    if (movimientos.isEmpty()) {
        item {
            Text(
                "Todavía no hay movimientos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    } else {
        items(movimientos, key = { it.id }) { movimiento ->
            FilaMovimiento(movimiento, unidad)
            HorizontalDivider()
        }
    }
}

@Composable
private fun FilaMovimiento(movimiento: MovimientoRespuesta, unidad: Unidad) {
    val titulo = when (movimiento.tipo) {
        TipoMovimiento.ENTRADA -> "Entrada"
        TipoMovimiento.PRODUCCION -> "Producción #${movimiento.produccionId}"
        TipoMovimiento.VENTA -> "Venta #${movimiento.ventaId}"
        TipoMovimiento.MERMA -> "Merma"
        TipoMovimiento.AJUSTE -> "Ajuste"
    }
    // La cantidad se lee de dos formas: por el signo (+/-) y por el color (verde si entra, rojo si sale).
    val colorCantidad = when (movimiento.cantidad.signum()) {
        1 -> MaterialTheme.colorScheme.entrada
        -1 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = {
            Column {
                Text("${formatearFechaHora(movimiento.fecha)} · ${movimiento.usuario}")
                movimiento.motivo?.let { Text("«$it»") }
            }
        },
        trailingContent = {
            Text(
                formatearCantidadConSigno(movimiento.cantidad, unidad),
                style = MaterialTheme.typography.titleMedium,
                color = colorCantidad
            )
        }
    )
}
