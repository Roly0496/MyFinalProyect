package com.horneo.app.ui.produccion

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.R
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.ui.formatearCantidad

// Registrar producción (boceto 07): producto, cantidad y vista previa de lo que se consumirá.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarProduccionScreen(
    onCerrar: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrarProduccionViewModel = viewModel(factory = RegistrarProduccionViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onGuardado()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Registrar producción") },
            navigationIcon = {
                IconButton(onClick = onCerrar) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Cerrar sin guardar")
                }
            }
        )

        when {
            estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

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

            else -> Formulario(
                estado = estado,
                onProducto = viewModel::elegirProducto,
                onCantidad = viewModel::cambiarCantidad,
                onGuardar = viewModel::guardar,
            )
        }
    }
}

@Composable
private fun Formulario(
    estado: RegistrarProduccionUiState,
    onProducto: (ProductoRespuesta) -> Unit,
    onCantidad: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SelectorProducto(estado.productos, estado.productoElegido, estado.errorProducto, onProducto)

        OutlinedTextField(
            value = estado.cantidad,
            onValueChange = onCantidad,
            label = { Text("Cantidad fabricada") },
            suffix = { Text("ud") },
            singleLine = true,
            isError = estado.errorCantidad != null,
            supportingText = estado.errorCantidad?.let { error -> { Text(error) } },
            // Solo números enteros: no se fabrica media unidad.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        val receta = estado.receta
        val cantidad = RegistrarProduccionViewModel.cantidadValida(estado.cantidad)
        var hayFaltas = false
        when {
            estado.productoElegido == null -> Unit
            estado.errorReceta != null -> Text(estado.errorReceta, color = MaterialTheme.colorScheme.error)
            receta == null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            receta.isEmpty() -> Text(
                "Este producto no tiene receta: no se puede producir hasta que se le añada una.",
                color = MaterialTheme.colorScheme.error
            )
            // La vista previa solo se muestra cuando ya hay una cantidad válida.
            cantidad != null -> {
                val consumo = calcularConsumo(receta, cantidad, estado.stocks)
                val faltan = consumo.filter { !it.alcanza }
                hayFaltas = faltan.isNotEmpty()
                VistaPrevia(consumo)
                if (hayFaltas) {
                    // El aviso dice qué falta y cómo arreglarlo, no solo que hay un error.
                    val maximo = maximoProducible(receta, estado.stocks)
                    val nombres = faltan.joinToString(" y ") { it.nombre.lowercase() }
                    val solucion = if (maximo > 0) "Registra una entrada o produce como máximo $maximo ud."
                    else "Registra una entrada antes de producir."
                    Text("No hay suficiente $nombres. $solucion", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        estado.errorEnvio?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onGuardar,
            // Desactivado si falta materia prima o no hay receta: no tiene sentido intentarlo.
            enabled = !estado.enviando && !hayFaltas && receta?.isEmpty() != true,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Registrar producción")
            }
        }
    }
}

// Desplegable con los productos elaborados.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorProducto(
    productos: List<ProductoRespuesta>,
    elegido: ProductoRespuesta?,
    error: String?,
    onElegir: (ProductoRespuesta) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = elegido?.nombre.orEmpty(),
            onValueChange = {},
            // Solo lectura: el producto se elige de la lista, no se escribe.
            readOnly = true,
            label = { Text("Producto") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            productos.forEach { producto ->
                DropdownMenuItem(
                    text = { Text(producto.nombre) },
                    onClick = {
                        onElegir(producto)
                        abierto = false
                    }
                )
            }
        }
    }
}

// "Se consumirá": una fila por materia prima. Las que no alcanzan van con fondo de error
// y dicen cuánto falta (texto además de color, para que se entienda sin distinguir el rojo).
@Composable
private fun VistaPrevia(consumo: List<ConsumoIngrediente>) {
    Column {
        Text("Se consumirá", style = MaterialTheme.typography.titleMedium)
        consumo.forEach { ingrediente ->
            val hay = "Hay ${formatearCantidad(ingrediente.disponible, ingrediente.unidad)}"
            val colores = if (ingrediente.alcanza) ListItemDefaults.colors() else ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                headlineColor = MaterialTheme.colorScheme.onErrorContainer,
                supportingColor = MaterialTheme.colorScheme.onErrorContainer,
                trailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            )
            ListItem(
                headlineContent = { Text(ingrediente.nombre) },
                supportingContent = {
                    Text(
                        if (ingrediente.alcanza) hay
                        else "$hay · faltan ${formatearCantidad(ingrediente.falta, ingrediente.unidad)}"
                    )
                },
                trailingContent = {
                    Text(
                        formatearCantidad(ingrediente.necesario, ingrediente.unidad),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ingrediente.alcanza) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                },
                colors = colores,
            )
            HorizontalDivider()
        }
    }
}
