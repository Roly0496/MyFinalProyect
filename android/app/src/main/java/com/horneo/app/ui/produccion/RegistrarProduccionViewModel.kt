package com.horneo.app.ui.produccion

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
import com.horneo.app.data.remote.ProductoRespuesta
import com.horneo.app.data.remote.TipoProducto
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.data.repository.ProduccionRepository
import com.horneo.app.data.repository.ProductoRepository
import com.horneo.app.ui.navegacion.RegistrarProduccionRuta
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class RegistrarProduccionUiState(
    val cargando: Boolean = true,
    val errorCarga: String? = null,
    // Solo los productos elaborados: los de reventa no se fabrican.
    val productos: List<ProductoRespuesta> = emptyList(),
    // Stock actual de cada materia prima (id → stock), para la vista previa.
    val stocks: Map<Long, BigDecimal> = emptyMap(),
    val productoElegido: ProductoRespuesta? = null,
    // Receta del producto elegido (null mientras se carga).
    val receta: List<IngredienteRespuesta>? = null,
    val errorReceta: String? = null,
    val cantidad: String = "",
    val errorCantidad: String? = null,
    val errorProducto: String? = null,
    val enviando: Boolean = false,
    val errorEnvio: String? = null,
    val guardado: Boolean = false,
)

class RegistrarProduccionViewModel(
    savedStateHandle: SavedStateHandle,
    private val productoRepository: ProductoRepository,
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val produccionRepository: ProduccionRepository,
) : ViewModel() {

    // Si se abre desde el detalle de un producto, llega su id para dejarlo ya elegido.
    private val productoInicial = savedStateHandle.toRoute<RegistrarProduccionRuta>().productoId

    private val _estado = MutableStateFlow(RegistrarProduccionUiState())
    val estado: StateFlow<RegistrarProduccionUiState> = _estado.asStateFlow()

    private var cargaReceta: Job? = null

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, errorCarga = null) }
        viewModelScope.launch {
            // Productos y materias primas a la vez: los dos hacen falta antes de poder empezar.
            val productos = async { productoRepository.listar() }
            val materiasPrimas = async { materiaPrimaRepository.listar() }
            val resultadoProductos = productos.await()
            val resultadoMateriasPrimas = materiasPrimas.await()

            val error = resultadoProductos.exceptionOrNull() ?: resultadoMateriasPrimas.exceptionOrNull()
            if (error != null) {
                _estado.update { it.copy(cargando = false, errorCarga = mensajeDeError(error)) }
                return@launch
            }
            val elaborados = resultadoProductos.getOrThrow().filter { it.tipo == TipoProducto.ELABORADO }
            val stocks = resultadoMateriasPrimas.getOrThrow().associate { it.id to it.stockActual }
            _estado.update { it.copy(cargando = false, productos = elaborados, stocks = stocks) }

            elaborados.find { it.id == productoInicial }?.let { elegirProducto(it) }
        }
    }

    fun elegirProducto(producto: ProductoRespuesta) {
        _estado.update {
            it.copy(productoElegido = producto, receta = null, errorReceta = null, errorProducto = null, errorEnvio = null)
        }
        // Si se cambia de producto antes de que llegue la receta anterior, esa petición se cancela.
        cargaReceta?.cancel()
        cargaReceta = viewModelScope.launch {
            productoRepository.receta(producto.id).fold(
                onSuccess = { receta -> _estado.update { it.copy(receta = receta) } },
                onFailure = { error -> _estado.update { it.copy(errorReceta = mensajeDeError(error)) } }
            )
        }
    }

    fun cambiarCantidad(texto: String) {
        _estado.update { it.copy(cantidad = texto, errorCantidad = null, errorEnvio = null) }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.enviando) return

        val producto = actual.productoElegido
        val cantidad = cantidadValida(actual.cantidad)
        val errorProducto = if (producto == null) "Elige el producto que se ha fabricado" else null
        val errorCantidad = if (cantidad == null) "Escribe un número entero mayor que 0" else null
        if (producto == null || cantidad == null) {
            _estado.update { it.copy(errorProducto = errorProducto, errorCantidad = errorCantidad) }
            return
        }
        // Si falta alguna materia prima no se envía (el botón ya sale desactivado en ese caso).
        val receta = actual.receta ?: return
        if (calcularConsumo(receta, cantidad, actual.stocks).any { !it.alcanza }) return

        _estado.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            produccionRepository.registrar(producto.id, cantidad).fold(
                onSuccess = { _estado.update { it.copy(enviando = false, guardado = true) } },
                // Por ejemplo, si otra persona gastó una materia prima justo antes.
                onFailure = { error -> _estado.update { it.copy(enviando = false, errorEnvio = mensajeDeError(error)) } }
            )
        }
    }

    companion object {
        // Solo números enteros mayores que 0 (no se fabrica media unidad). Devuelve null si no vale.
        fun cantidadValida(texto: String): Int? = texto.trim().toIntOrNull()?.takeIf { it > 0 }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                RegistrarProduccionViewModel(
                    createSavedStateHandle(),
                    app.container.productoRepository,
                    app.container.materiaPrimaRepository,
                    app.container.produccionRepository,
                )
            }
        }
    }
}
