package com.example.myfinalproyect.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlmacenDao {
    @Query("SELECT * FROM almacen_items ORDER BY nombre ASC")
    fun getAll(): Flow<List<AlmacenItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AlmacenItem): Long

    @Update
    suspend fun update(item: AlmacenItem)

    @Delete
    suspend fun delete(item: AlmacenItem)

    @Query("UPDATE almacen_items SET cantidad = cantidad - :cantidad WHERE id = :id")
    suspend fun restarCantidad(id: Long, cantidad: Double)
}
