package com.horneo.app.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.ProductoRepository
import com.horneo.app.data.repository.VentaRepository
import com.horneo.app.ui.componentes.ListaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevaVentaUiState(
    val productos: ListaUiState<ProductoRespuesta> = ListaUiState.Cargando,
    val carrito: Carrito = emptyMap(),
    val enviando: Boolean = false,
    val errorEnvio: String? = null,
    // Id de la venta registrada: cuando llega, la pantalla abre su ticket.
    val ventaRegistrada: Long? = null,
)

class NuevaVentaViewModel(
    private val productoRepository: ProductoRepository,
    private val ventaRepository: VentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(NuevaVentaUiState())
    val estado: StateFlow<NuevaVentaUiState> = _estado.asStateFlow()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            val resultado = productoRepository.listar()
            _estado.update { actual ->
                resultado.fold(
                    onSuccess = { productos ->
                        // Con el stock nuevo, el carrito se recorta si hace falta.
                        actual.copy(productos = ListaUiState.Listo(productos), carrito = actual.carrito.ajustarAlStock(productos))
                    },
                    onFailure = { error -> actual.copy(productos = ListaUiState.Error(mensajeDeError(error))) }
                )
            }
        }
    }

    fun sumar(producto: ProductoRespuesta) {
        _estado.update { it.copy(carrito = it.carrito.sumar(producto), errorEnvio = null) }
    }

    fun restar(producto: ProductoRespuesta) {
        _estado.update { it.copy(carrito = it.carrito.restar(producto.id), errorEnvio = null) }
    }

    fun cobrar() {
        val actual = _estado.value
        if (actual.enviando || actual.carrito.isEmpty()) return
        _estado.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            ventaRepository.registrar(actual.carrito).fold(
                onSuccess = { ticket -> _estado.update { it.copy(enviando = false, ventaRegistrada = ticket.venta.id) } },
                onFailure = { error ->
                    // Lo más probable es que otra persona haya vendido a la vez y ya no quede stock:
                    // se muestra el mensaje del backend y se vuelve a pedir el stock para ajustar el carrito.
                    _estado.update { it.copy(enviando = false, errorEnvio = mensajeDeError(error)) }
                    cargarProductos()
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                NuevaVentaViewModel(app.container.productoRepository, app.container.ventaRepository)
            }
        }
    }
}
