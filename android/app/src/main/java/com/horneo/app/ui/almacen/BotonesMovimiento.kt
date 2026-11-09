package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Botones Entrada / Merma / Ajuste que comparten el detalle de materia prima y el de producto.
// Tienen distinto peso visual, como en el boceto: la entrada es la acción más habitual (relleno),
// la merma va después (tonal) y el ajuste es la menos frecuente (con borde).
// Los botones que no tocan no aparecen, en lugar de salir desactivados: un botón que no se puede
// pulsar confunde. Aun así, el backend rechaza cualquier movimiento que no esté permitido.
@Composable
internal fun BotonesMovimiento(
    // La entrada no existe para los productos elaborados: su stock sube con una producción.
    mostrarEntrada: Boolean,
    // El ajuste es solo para el administrador.
    mostrarAjuste: Boolean,
    onMovimiento: (TipoFormularioMovimiento) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mostrarEntrada) {
            Button(onClick = { onMovimiento(TipoFormularioMovimiento.ENTRADA) }, modifier = Modifier.weight(1f)) {
                Text("Entrada")
            }
        }
        FilledTonalButton(onClick = { onMovimiento(TipoFormularioMovimiento.MERMA) }, modifier = Modifier.weight(1f)) {
            Text("Merma")
        }
        if (mostrarAjuste) {
            OutlinedButton(onClick = { onMovimiento(TipoFormularioMovimiento.AJUSTE) }, modifier = Modifier.weight(1f)) {
                Text("Ajuste")
            }
        }
    }
}
