package com.horneo.app.ui.almacen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.MateriaPrimaRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Los tres estados posibles de la lista de materias primas.
sealed interface MateriasPrimasUiState {
    data object Cargando : MateriasPrimasUiState
    data class Error(val mensaje: String) : MateriasPrimasUiState
    data class Listo(val materiasPrimas: List<MateriaPrimaRespuesta>) : MateriasPrimasUiState
}

class AlmacenViewModel(private val materiaPrimaRepository: MateriaPrimaRepository) : ViewModel() {

    private val _materiasPrimas = MutableStateFlow<MateriasPrimasUiState>(MateriasPrimasUiState.Cargando)
    val materiasPrimas: StateFlow<MateriasPrimasUiState> = _materiasPrimas.asStateFlow()

    init {
        cargar()
    }

    // Se llama al abrir la pantalla y al pulsar "Reintentar".
    fun cargar() {
        _materiasPrimas.value = MateriasPrimasUiState.Cargando
        viewModelScope.launch {
            _materiasPrimas.value = materiaPrimaRepository.listar().fold(
                onSuccess = { MateriasPrimasUiState.Listo(it) },
                onFailure = { MateriasPrimasUiState.Error(mensajeDeError(it)) }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                AlmacenViewModel(app.container.materiaPrimaRepository)
            }
        }
    }
}
