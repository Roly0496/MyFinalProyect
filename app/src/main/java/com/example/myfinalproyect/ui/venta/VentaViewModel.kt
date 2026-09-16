package com.example.myfinalproyect.ui.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myfinalproyect.InventarioApplication
import com.example.myfinalproyect.data.InventarioRepository
import com.example.myfinalproyect.data.ProductoEnVenta
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VentaViewModel(private val repository: InventarioRepository) : ViewModel() {

    val items: StateFlow<List<ProductoEnVenta>> = repository.productosEnVenta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(nombre: String, cantidad: Int, precio: Double?) {
        viewModelScope.launch {
            repository.addProductoEnVenta(ProductoEnVenta(nombre = nombre, cantidad = cantidad, precio = precio))
        }
    }

    fun updateItem(item: ProductoEnVenta) {
        viewModelScope.launch { repository.updateProductoEnVenta(item) }
    }

    fun deleteItem(item: ProductoEnVenta) {
        viewModelScope.launch { repository.deleteProductoEnVenta(item) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as InventarioApplication
                VentaViewModel(app.repository)
            }
        }
    }
}
