package com.horneo.app.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.CajaRespuesta
import com.horneo.app.data.remote.VentaRespuesta
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.VentaRepository
import com.horneo.app.ui.componentes.ListaUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class VentasViewModel(
    private val esAdmin: Boolean,
    private val ventaRepository: VentaRepository,
) : ViewModel() {

    private val _dia = MutableStateFlow(LocalDate.now())
    val dia: StateFlow<LocalDate> = _dia.asStateFlow()

    private val _ventas = MutableStateFlow<ListaUiState<VentaRespuesta>>(ListaUiState.Cargando)
    val ventas: StateFlow<ListaUiState<VentaRespuesta>> = _ventas.asStateFlow()

    // Caja del día: solo para el administrador (para el empleado se queda en null).
    private val _caja = MutableStateFlow<CajaRespuesta?>(null)
    val caja: StateFlow<CajaRespuesta?> = _caja.asStateFlow()

    // Igual que en Producción: al cambiar de día se cancela la petición anterior,
    // para que una respuesta lenta no pise la del día que se está mirando.
    private var carga: Job? = null

    fun elegirDia(nuevoDia: LocalDate) {
        if (nuevoDia.isAfter(LocalDate.now()) || nuevoDia == _dia.value) return
        _dia.value = nuevoDia
        _ventas.value = ListaUiState.Cargando
        _caja.value = null
        cargar()
    }

    // La pantalla lo llama cada vez que se vuelve visible, y también "Reintentar".
    fun cargar() {
        if (_ventas.value !is ListaUiState.Listo) _ventas.value = ListaUiState.Cargando
        carga?.cancel()
        carga = viewModelScope.launch {
            val dia = _dia.value
            // La lista y la caja a la vez. La caja solo se pide si el usuario es administrador:
            // a un empleado el backend le respondería 403.
            val ventas = async { ventaRepository.listarDelDia(dia) }
            val caja = if (esAdmin) async { ventaRepository.caja(dia) } else null
            val resultadoVentas = ventas.await()
            val resultadoCaja = caja?.await()

            val error = resultadoVentas.exceptionOrNull() ?: resultadoCaja?.exceptionOrNull()
            if (error != null) {
                _ventas.value = ListaUiState.Error(mensajeDeError(error))
            } else {
                _caja.value = resultadoCaja?.getOrThrow()
                _ventas.value = ListaUiState.Listo(resultadoVentas.getOrThrow())
            }
        }
    }

    companion object {
        // Como en el detalle de producto, la fábrica recibe el rol para decidir si se pide la caja.
        fun factory(esAdmin: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                VentasViewModel(esAdmin, app.container.ventaRepository)
            }
        }
    }
}
