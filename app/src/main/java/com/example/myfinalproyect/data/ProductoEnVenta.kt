package com.example.myfinalproyect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos_en_venta")
data class ProductoEnVenta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val cantidad: Int,
    val precio: Double? = null
)
