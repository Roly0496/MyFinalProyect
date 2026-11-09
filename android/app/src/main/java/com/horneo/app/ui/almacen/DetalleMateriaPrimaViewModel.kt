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
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.ui.navegacion.DetalleMateriaPrimaRuta
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetalleMateriaPrimaUiState {
    data object Cargando : DetalleMateriaPrimaUiState
    data class Error(val mensaje: String) : DetalleMateriaPrimaUiState
    data class Listo(
        val materiaPrima: MateriaPrimaRespuesta,
        val movimientos: List<MovimientoRespuesta>,
    ) : DetalleMateriaPrimaUiState
}

class DetalleMateriaPrimaViewModel(
    savedStateHandle: SavedStateHandle,
    private val materiaPrimaRepository: MateriaPrimaRepository,
) : ViewModel() {

    // La ruta de navegación (con el id) se guarda en el SavedStateHandle del ViewModel.
    // toRoute la recupera, así el ViewModel sabe qué materia prima tiene que pedir.
    private val id = savedStateHandle.toRoute<DetalleMateriaPrimaRuta>().id

    private val _estado = MutableStateFlow<DetalleMateriaPrimaUiState>(DetalleMateriaPrimaUiState.Cargando)
    val estado: StateFlow<DetalleMateriaPrimaUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = DetalleMateriaPrimaUiState.Cargando
        viewModelScope.launch {
            // async lanza las dos peticiones a la vez en lugar de una detrás de otra,
            // y await espera a que termine cada una.
            val materiaPrima = async { materiaPrimaRepository.obtener(id) }
            val movimientos = async { materiaPrimaRepository.movimientos(id) }
            val resultadoMateriaPrima = materiaPrima.await()
            val resultadoMovimientos = movimientos.await()

            // Si falla cualquiera de las dos, se muestra el error (con «Reintentar»).
            val error = resultadoMateriaPrima.exceptionOrNull() ?: resultadoMovimientos.exceptionOrNull()
            _estado.value = if (error != null) {
                DetalleMateriaPrimaUiState.Error(mensajeDeError(error))
            } else {
                DetalleMateriaPrimaUiState.Listo(resultadoMateriaPrima.getOrThrow(), resultadoMovimientos.getOrThrow())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                DetalleMateriaPrimaViewModel(createSavedStateHandle(), app.container.materiaPrimaRepository)
            }
        }
    }
}
