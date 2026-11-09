package com.horneo.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Los tres estados posibles de una lista que se pide al servidor.
// Es genérica (<T>) para usar la misma con materias primas, productos, producciones...
sealed interface ListaUiState<out T> {
    data object Cargando : ListaUiState<Nothing>
    data class Error(val mensaje: String) : ListaUiState<Nothing>
    data class Listo<T>(val elementos: List<T>) : ListaUiState<T>
}

// Dibuja cualquier lista según su estado: cargando, error, vacía o con datos.
// Cada pantalla solo tiene que decir cómo se dibuja una fila (el parámetro fila).
@Composable
fun <T> ContenidoLista(
    estado: ListaUiState<T>,
    textoVacio: String,
    onReintentar: () -> Unit,
    clave: (T) -> Any,
    modifier: Modifier = Modifier,
    // Espacio extra al final de la lista, por ejemplo para que un botón flotante no tape la última fila.
    margenInferior: PaddingValues = PaddingValues(),
    fila: @Composable (T) -> Unit,
) {
    when (estado) {
        ListaUiState.Cargando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ListaUiState.Error -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(estado.mensaje, textAlign = TextAlign.Center)
            Button(onClick = onReintentar) { Text("Reintentar") }
        }

        is ListaUiState.Listo ->
            if (estado.elementos.isEmpty()) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(textoVacio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // LazyColumn solo dibuja las filas visibles en pantalla (como un RecyclerView).
                LazyColumn(modifier.fillMaxSize(), contentPadding = margenInferior) {
                    items(estado.elementos, key = clave) { elemento ->
                        fila(elemento)
                        HorizontalDivider()
                    }
                }
            }
    }
}
