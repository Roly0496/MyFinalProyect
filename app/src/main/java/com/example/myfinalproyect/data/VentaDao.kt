package com.example.myfinalproyect.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {
    @Query("SELECT * FROM productos_en_venta ORDER BY nombre ASC")
    fun getAll(): Flow<List<ProductoEnVenta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ProductoEnVenta): Long

    @Update
    suspend fun update(item: ProductoEnVenta)

    @Delete
    suspend fun delete(item: ProductoEnVenta)

    @Query("UPDATE productos_en_venta SET cantidad = cantidad + :cantidad WHERE id = :id")
    suspend fun sumarCantidad(id: Long, cantidad: Int)
}
