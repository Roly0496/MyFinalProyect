package com.example.myfinalproyect.ui.almacen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myfinalproyect.InventarioApplication
import com.example.myfinalproyect.data.AlmacenItem
import com.example.myfinalproyect.data.InventarioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlmacenViewModel(private val repository: InventarioRepository) : ViewModel() {

    val items: StateFlow<List<AlmacenItem>> = repository.almacenItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(nombre: String, cantidad: Double, unidad: String) {
        viewModelScope.launch {
            repository.addAlmacenItem(AlmacenItem(nombre = nombre, cantidad = cantidad, unidad = unidad))
        }
    }

    fun updateItem(item: AlmacenItem) {
        viewModelScope.launch { repository.updateAlmacenItem(item) }
    }

    fun deleteItem(item: AlmacenItem) {
        viewModelScope.launch { repository.deleteAlmacenItem(item) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as InventarioApplication
                AlmacenViewModel(app.repository)
            }
        }
    }
}
