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
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.data.repository.MovimientoRepository
import com.horneo.app.ui.navegacion.FormularioMovimientoRuta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioMovimientoUiState(
    // Se pide al abrir el formulario, para tener el stock actualizado (y su unidad).
    val materiaPrima: MateriaPrimaRespuesta? = null,
    val errorCarga: String? = null,
    // Lo que el usuario escribe, tal cual (texto): se convierte en número al validar.
    val cantidad: String = "",
    val motivo: String = "",
    val errorCantidad: String? = null,
    val errorMotivo: String? = null,
    val enviando: Boolean = false,
    // Error que devuelve el backend al guardar (por ejemplo, si otro usuario gastó el stock antes).
    val errorEnvio: String? = null,
    val guardado: Boolean = false,
)

class FormularioMovimientoViewModel(
    savedStateHandle: SavedStateHandle,
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val movimientoRepository: MovimientoRepository,
) : ViewModel() {

    private val ruta = savedStateHandle.toRoute<FormularioMovimientoRuta>()
    val tipo: TipoFormularioMovimiento = ruta.tipo

    private val _estado = MutableStateFlow(FormularioMovimientoUiState())
    val estado: StateFlow<FormularioMovimientoUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(errorCarga = null) }
        viewModelScope.launch {
            materiaPrimaRepository.obtener(ruta.id).fold(
                onSuccess = { materiaPrima -> _estado.update { it.copy(materiaPrima = materiaPrima) } },
                onFailure = { error -> _estado.update { it.copy(errorCarga = mensajeDeError(error)) } }
            )
        }
    }

    // Al escribir se borra el error de ese campo: ya no se refiere a lo que hay escrito.
    fun cambiarCantidad(texto: String) {
        _estado.update { it.copy(cantidad = texto, errorCantidad = null, errorEnvio = null) }
    }

    fun cambiarMotivo(texto: String) {
        _estado.update { it.copy(motivo = texto, errorMotivo = null, errorEnvio = null) }
    }

    fun guardar() {
        val actual = _estado.value
        val materiaPrima = actual.materiaPrima ?: return
        if (actual.enviando) return  // evita enviar dos veces si se pulsa el botón muy rápido

        val validacion = validarMovimiento(tipo, actual.cantidad, actual.motivo, materiaPrima.stockActual, materiaPrima.unidad)
        if (!validacion.esValido || validacion.cantidad == null) {
            _estado.update { it.copy(errorCantidad = validacion.errorCantidad, errorMotivo = validacion.errorMotivo) }
            return
        }

        val cantidad = validacion.cantidad
        val motivo = actual.motivo.trim()
        _estado.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            val resultado = when (tipo) {
                // En la entrada el motivo es opcional: si está vacío se envía null.
                TipoFormularioMovimiento.ENTRADA ->
                    movimientoRepository.registrarEntradaMateriaPrima(materiaPrima.id, cantidad, motivo.ifEmpty { null })
                TipoFormularioMovimiento.MERMA ->
                    movimientoRepository.registrarMermaMateriaPrima(materiaPrima.id, cantidad, motivo)
                TipoFormularioMovimiento.AJUSTE ->
                    movimientoRepository.ajustarStockMateriaPrima(materiaPrima.id, cantidad, motivo)
            }
            resultado.fold(
                onSuccess = { _estado.update { it.copy(enviando = false, guardado = true) } },
                onFailure = { error -> _estado.update { it.copy(enviando = false, errorEnvio = mensajeDeError(error)) } }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                FormularioMovimientoViewModel(
                    createSavedStateHandle(),
                    app.container.materiaPrimaRepository,
                    app.container.movimientoRepository,
                )
            }
        }
    }
}
