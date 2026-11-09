package com.horneo.app.ui.almacen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.IngredienteRespuesta
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.TipoProducto
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.ProductoRepository
import com.horneo.app.ui.navegacion.DetalleProductoRuta
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface DetalleProductoUiState {
    data object Cargando : DetalleProductoUiState
    data class Error(val mensaje: String) : DetalleProductoUiState
    data class Listo(
        val producto: ProductoRespuesta,
        val receta: List<IngredienteRespuesta>,
        // null para el empleado: no puede ver costes.
        val coste: BigDecimal?,
        val movimientos: List<MovimientoRespuesta>,
    ) : DetalleProductoUiState
}

class DetalleProductoViewModel(
    savedStateHandle: SavedStateHandle,
    private val esAdmin: Boolean,
    private val productoRepository: ProductoRepository,
) : ViewModel() {

    private val id = savedStateHandle.toRoute<DetalleProductoRuta>().id

    private val _estado = MutableStateFlow<DetalleProductoUiState>(DetalleProductoUiState.Cargando)
    val estado: StateFlow<DetalleProductoUiState> = _estado.asStateFlow()

    // La pantalla lo llama cada vez que se vuelve visible (también la primera).
    fun cargar() {
        if (_estado.value !is DetalleProductoUiState.Listo) {
            _estado.value = DetalleProductoUiState.Cargando
        }
        viewModelScope.launch {
            // Las peticiones van a la vez. El coste solo se pide si el usuario es administrador:
            // a un empleado el backend le respondería 403, así que ni se intenta.
            val producto = async { productoRepository.obtener(id) }
            val movimientos = async { productoRepository.movimientos(id) }
            val coste = if (esAdmin) async { productoRepository.coste(id) } else null

            val resultadoProducto = producto.await()
            // La receta solo existe en los elaborados: para uno de reventa el backend responde 409
            // («no tiene receta»). Como el tipo no se sabe hasta que llega el producto, la receta se
            // pide después y solo si hace falta.
            val resultadoReceta = when (resultadoProducto.getOrNull()?.tipo) {
                TipoProducto.ELABORADO -> productoRepository.receta(id)
                else -> Result.success(emptyList())
            }
            val resultadoMovimientos = movimientos.await()
            val resultadoCoste = coste?.await()

            val error = resultadoProducto.exceptionOrNull()
                ?: resultadoReceta.exceptionOrNull()
                ?: resultadoMovimientos.exceptionOrNull()
                ?: resultadoCoste?.exceptionOrNull()
            _estado.value = if (error != null) {
                DetalleProductoUiState.Error(mensajeDeError(error))
            } else {
                DetalleProductoUiState.Listo(
                    producto = resultadoProducto.getOrThrow(),
                    receta = resultadoReceta.getOrThrow(),
                    coste = resultadoCoste?.getOrThrow()?.coste,
                    movimientos = resultadoMovimientos.getOrThrow(),
                )
            }
        }
    }

    companion object {
        // A diferencia de las otras pantallas, la fábrica recibe un dato (esAdmin), porque el
        // ViewModel necesita saber el rol para decidir si pide el coste.
        fun factory(esAdmin: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                DetalleProductoViewModel(createSavedStateHandle(), esAdmin, app.container.productoRepository)
            }
        }
    }
}
