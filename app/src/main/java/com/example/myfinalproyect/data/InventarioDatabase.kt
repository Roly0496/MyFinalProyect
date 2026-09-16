package com.example.myfinalproyect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlmacenItem::class, ProductoEnVenta::class],
    version = 1,
    exportSchema = false
)
abstract class InventarioDatabase : RoomDatabase() {
    abstract fun almacenDao(): AlmacenDao
    abstract fun ventaDao(): VentaDao

    companion object {
        @Volatile
        private var INSTANCE: InventarioDatabase? = null

        fun getInstance(context: Context): InventarioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InventarioDatabase::class.java,
                    "inventario.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
