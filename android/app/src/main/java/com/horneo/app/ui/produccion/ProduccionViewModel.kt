package com.horneo.app.ui.produccion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.ProduccionRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.ProduccionRepository
import com.horneo.app.ui.componentes.ListaUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProduccionViewModel(private val produccionRepository: ProduccionRepository) : ViewModel() {

    // Día que se está mirando. Empieza en hoy.
    private val _dia = MutableStateFlow(LocalDate.now())
    val dia: StateFlow<LocalDate> = _dia.asStateFlow()

    private val _producciones = MutableStateFlow<ListaUiState<ProduccionRespuesta>>(ListaUiState.Cargando)
    val producciones: StateFlow<ListaUiState<ProduccionRespuesta>> = _producciones.asStateFlow()

    // La petición en curso. Si se cambia de día antes de que llegue la respuesta, se cancela:
    // así una respuesta lenta de un día anterior no puede pisar la del día que se está mirando.
    private var carga: Job? = null

    fun elegirDia(nuevoDia: LocalDate) {
        // No se puede mirar el futuro: todavía no se ha producido nada.
        if (nuevoDia.isAfter(LocalDate.now()) || nuevoDia == _dia.value) return
        _dia.value = nuevoDia
        // Es otro día: la lista anterior ya no vale, así que se muestra el círculo de carga.
        _producciones.value = ListaUiState.Cargando
        cargar()
    }

    // La pantalla lo llama cada vez que se vuelve visible, y también "Reintentar".
    // Si ya hay datos del mismo día, se siguen mostrando mientras llegan los nuevos (sin parpadeo).
    fun cargar() {
        if (_producciones.value !is ListaUiState.Listo) _producciones.value = ListaUiState.Cargando
        carga?.cancel()
        carga = viewModelScope.launch {
            _producciones.value = produccionRepository.listarDelDia(_dia.value).fold(
                onSuccess = { ListaUiState.Listo(it) },
                onFailure = { ListaUiState.Error(mensajeDeError(it)) }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                ProduccionViewModel(app.container.produccionRepository)
            }
        }
    }
}
