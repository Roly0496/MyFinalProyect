package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.ui.formatearCantidad

// Formulario de entrada, merma o ajuste de una materia prima (boceto 05).
// La misma pantalla sirve para los tres: los textos y las reglas cambian según el tipo.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioMovimientoScreen(
    nombre: String,
    onCerrar: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormularioMovimientoViewModel = viewModel(factory = FormularioMovimientoViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val tipo = viewModel.tipo

    // Cuando el backend confirma el movimiento, se cierra el formulario.
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onGuardado()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${tipo.titulo} · $nombre") },
            navigationIcon = {
                // Una X en lugar de una flecha: indica que se sale sin guardar.
                IconButton(onClick = onCerrar) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Cerrar sin guardar")
                }
            }
        )

        val materiaPrima = estado.materiaPrima
        when {
            estado.errorCarga != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estado.errorCarga.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = viewModel::cargar) { Text("Reintentar") }
            }

            materiaPrima == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> CamposFormulario(
                tipo = tipo,
                materiaPrima = materiaPrima,
                estado = estado,
                onCantidad = viewModel::cambiarCantidad,
                onMotivo = viewModel::cambiarMotivo,
                onGuardar = viewModel::guardar,
            )
        }
    }
}

@Composable
private fun CamposFormulario(
    tipo: TipoFormularioMovimiento,
    materiaPrima: MateriaPrimaRespuesta,
    estado: FormularioMovimientoUiState,
    onCantidad: (String) -> Unit,
    onMotivo: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    val unidad = materiaPrima.unidad
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Se puede desplazar e imePadding deja sitio al teclado, para que no tape el botón.
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Stock actual: ${formatearCantidad(materiaPrima.stockActual, unidad)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = estado.cantidad,
            onValueChange = onCantidad,
            label = { Text(tipo.etiquetaCantidad) },
            // La unidad aparece fija al lado del número: el usuario no la escribe ni la puede cambiar.
            suffix = { Text(unidad.simbolo) },
            singleLine = true,
            isError = estado.errorCantidad != null,
            // Debajo del campo: el error si lo hay (dice cómo arreglarlo) o, si no, la ayuda.
            supportingText = { Text(estado.errorCantidad ?: tipo.ayudaCantidad) },
            // Teclado numérico con separador decimal.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.motivo,
            onValueChange = onMotivo,
            label = { Text(if (tipo.motivoObligatorio) "Motivo" else "Motivo (opcional)") },
            isError = estado.errorMotivo != null,
            supportingText = estado.errorMotivo?.let { error -> { Text(error) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        // Vista previa: solo si lo escrito ya es un número válido.
        val validacion = validarMovimiento(tipo, estado.cantidad, estado.motivo, materiaPrima.stockActual, unidad)
        validacion.cantidad?.let { cantidad ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        "Después de guardar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatearCantidad(stockDespues(tipo, cantidad, materiaPrima.stockActual), unidad),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        estado.errorEnvio?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onGuardar,
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(tipo.textoBoton)
            }
        }
    }
}
