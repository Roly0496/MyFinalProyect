package com.example.myfinalproyect

import android.app.Application
import com.example.myfinalproyect.data.InventarioDatabase
import com.example.myfinalproyect.data.InventarioRepository

class InventarioApplication : Application() {
    private val database: InventarioDatabase by lazy { InventarioDatabase.getInstance(this) }
    val repository: InventarioRepository by lazy {
        InventarioRepository(database.almacenDao(), database.ventaDao())
    }
}
