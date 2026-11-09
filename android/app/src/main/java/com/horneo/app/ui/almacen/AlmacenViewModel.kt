package com.horneo.app.ui.almacen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Los tres estados posibles de una lista que se pide al servidor.
// Es genérica (<T>) para usar la misma con materias primas y con productos.
sealed interface ListaUiState<out T> {
    data object Cargando : ListaUiState<Nothing>
    data class Error(val mensaje: String) : ListaUiState<Nothing>
    data class Listo<T>(val elementos: List<T>) : ListaUiState<T>
}

class AlmacenViewModel(
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val productoRepository: ProductoRepository,
) : ViewModel() {

    private val _materiasPrimas = MutableStateFlow<ListaUiState<MateriaPrimaRespuesta>>(ListaUiState.Cargando)
    val materiasPrimas: StateFlow<ListaUiState<MateriaPrimaRespuesta>> = _materiasPrimas.asStateFlow()

    private val _productos = MutableStateFlow<ListaUiState<ProductoRespuesta>>(ListaUiState.Cargando)
    val productos: StateFlow<ListaUiState<ProductoRespuesta>> = _productos.asStateFlow()

    // No hay init: la pantalla llama a cargar() cada vez que se vuelve visible (también la primera),
    // así el stock está al día al volver del detalle. Se piden las dos listas a la vez, para que al
    // cambiar de pestaña ya estén cargadas.
    fun cargar() {
        cargarMateriasPrimas()
        cargarProductos()
    }

    // También se llaman al pulsar "Reintentar" en cada pestaña.
    // Si ya hay datos, se siguen mostrando mientras llegan los nuevos (sin parpadeo).
    fun cargarMateriasPrimas() {
        if (_materiasPrimas.value !is ListaUiState.Listo) _materiasPrimas.value = ListaUiState.Cargando
        viewModelScope.launch {
            _materiasPrimas.value = materiaPrimaRepository.listar().aEstado()
        }
    }

    fun cargarProductos() {
        if (_productos.value !is ListaUiState.Listo) _productos.value = ListaUiState.Cargando
        viewModelScope.launch {
            _productos.value = productoRepository.listar().aEstado()
        }
    }

    // Convierte el resultado del repositorio en el estado que dibuja la pantalla.
    private fun <T> Result<List<T>>.aEstado(): ListaUiState<T> = fold(
        onSuccess = { ListaUiState.Listo(it) },
        onFailure = { ListaUiState.Error(mensajeDeError(it)) }
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                AlmacenViewModel(app.container.materiaPrimaRepository, app.container.productoRepository)
            }
        }
    }
}
