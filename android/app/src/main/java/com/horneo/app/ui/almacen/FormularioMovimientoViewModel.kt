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
import com.horneo.app.data.remote.MovimientoRespuesta
import com.horneo.app.data.remote.Unidad
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.data.repository.MovimientoRepository
import com.horneo.app.data.repository.ProductoRepository
import com.horneo.app.ui.navegacion.FormularioMovimientoRuta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

// Lo que el formulario necesita saber del elemento, sea materia prima o producto:
// cuánto stock hay y en qué unidad. Así la pantalla no depende de cuál de los dos es.
data class StockElemento(val stockActual: BigDecimal, val unidad: Unidad)

data class FormularioMovimientoUiState(
    // Se pide al abrir el formulario, para tener el stock actualizado (y su unidad).
    val stock: StockElemento? = null,
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
    private val productoRepository: ProductoRepository,
    private val movimientoRepository: MovimientoRepository,
) : ViewModel() {

    private val ruta = savedStateHandle.toRoute<FormularioMovimientoRuta>()
    val tipo: TipoFormularioMovimiento = ruta.tipo
    val soloEnteros: Boolean = ruta.elemento == ElementoMovimiento.PRODUCTO

    private val _estado = MutableStateFlow(FormularioMovimientoUiState())
    val estado: StateFlow<FormularioMovimientoUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(errorCarga = null) }
        viewModelScope.launch {
            // Cada elemento se pide a su repositorio y se convierte en lo mismo: stock y unidad.
            val resultado = when (ruta.elemento) {
                ElementoMovimiento.MATERIA_PRIMA -> materiaPrimaRepository.obtener(ruta.id)
                    .map { StockElemento(it.stockActual, it.unidad) }
                // Los productos siempre se cuentan en unidades.
                ElementoMovimiento.PRODUCTO -> productoRepository.obtener(ruta.id)
                    .map { StockElemento(BigDecimal(it.stockActual), Unidad.UNIDAD) }
            }
            resultado.fold(
                onSuccess = { stock -> _estado.update { it.copy(stock = stock) } },
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
        val stock = actual.stock ?: return
        if (actual.enviando) return  // evita enviar dos veces si se pulsa el botón muy rápido

        val validacion = validarMovimiento(
            tipo, actual.cantidad, actual.motivo, stock.stockActual, stock.unidad, soloEnteros
        )
        if (!validacion.esValido || validacion.cantidad == null) {
            _estado.update { it.copy(errorCantidad = validacion.errorCantidad, errorMotivo = validacion.errorMotivo) }
            return
        }

        val cantidad = validacion.cantidad
        val motivo = actual.motivo.trim()
        _estado.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            val resultado = when (ruta.elemento) {
                ElementoMovimiento.MATERIA_PRIMA -> enviarMateriaPrima(cantidad, motivo)
                // La validación ya garantiza que es un número entero, así que intValueExact no falla.
                ElementoMovimiento.PRODUCTO -> enviarProducto(cantidad.intValueExact(), motivo)
            }
            resultado.fold(
                onSuccess = { _estado.update { it.copy(enviando = false, guardado = true) } },
                onFailure = { error -> _estado.update { it.copy(enviando = false, errorEnvio = mensajeDeError(error)) } }
            )
        }
    }

    // En la entrada el motivo es opcional: si está vacío se envía null.
    private suspend fun enviarMateriaPrima(cantidad: BigDecimal, motivo: String): Result<MovimientoRespuesta> =
        when (tipo) {
            TipoFormularioMovimiento.ENTRADA ->
                movimientoRepository.registrarEntradaMateriaPrima(ruta.id, cantidad, motivo.ifEmpty { null })
            TipoFormularioMovimiento.MERMA -> movimientoRepository.registrarMermaMateriaPrima(ruta.id, cantidad, motivo)
            TipoFormularioMovimiento.AJUSTE -> movimientoRepository.ajustarStockMateriaPrima(ruta.id, cantidad, motivo)
        }

    private suspend fun enviarProducto(cantidad: Int, motivo: String): Result<MovimientoRespuesta> =
        when (tipo) {
            TipoFormularioMovimiento.ENTRADA ->
                movimientoRepository.registrarEntradaProducto(ruta.id, cantidad, motivo.ifEmpty { null })
            TipoFormularioMovimiento.MERMA -> movimientoRepository.registrarMermaProducto(ruta.id, cantidad, motivo)
            TipoFormularioMovimiento.AJUSTE -> movimientoRepository.ajustarStockProducto(ruta.id, cantidad, motivo)
        }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                FormularioMovimientoViewModel(
                    createSavedStateHandle(),
                    app.container.materiaPrimaRepository,
                    app.container.productoRepository,
                    app.container.movimientoRepository,
                )
            }
        }
    }
}
