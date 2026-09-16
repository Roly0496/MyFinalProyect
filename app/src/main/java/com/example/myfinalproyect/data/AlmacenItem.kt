package com.example.myfinalproyect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "almacen_items")
data class AlmacenItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val cantidad: Double,
    val unidad: String
)
