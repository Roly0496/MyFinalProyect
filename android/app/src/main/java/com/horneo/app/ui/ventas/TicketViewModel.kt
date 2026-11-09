package com.horneo.app.ui.ventas

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
import com.horneo.app.data.remote.TicketRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.VentaRepository
import com.horneo.app.ui.navegacion.TicketRuta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TicketUiState {
    data object Cargando : TicketUiState
    data class Error(val mensaje: String) : TicketUiState
    data class Listo(val ticket: TicketRespuesta) : TicketUiState
}

// El ticket siempre se pide al backend por su id, tanto al cobrar como al abrir una venta de la
// lista. Así hay una sola forma de cargarlo y lo que se ve es siempre lo que está guardado.
class TicketViewModel(
    savedStateHandle: SavedStateHandle,
    private val ventaRepository: VentaRepository,
) : ViewModel() {

    private val ventaId = savedStateHandle.toRoute<TicketRuta>().ventaId

    private val _estado = MutableStateFlow<TicketUiState>(TicketUiState.Cargando)
    val estado: StateFlow<TicketUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = TicketUiState.Cargando
        viewModelScope.launch {
            _estado.value = ventaRepository.ticket(ventaId).fold(
                onSuccess = { TicketUiState.Listo(it) },
                onFailure = { TicketUiState.Error(mensajeDeError(it)) }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                TicketViewModel(createSavedStateHandle(), app.container.ventaRepository)
            }
        }
    }
}
