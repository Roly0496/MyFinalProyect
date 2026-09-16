package com.example.myfinalproyect.data

import kotlinx.coroutines.flow.Flow

class InventarioRepository(
    private val almacenDao: AlmacenDao,
    private val ventaDao: VentaDao
) {
    val almacenItems: Flow<List<AlmacenItem>> = almacenDao.getAll()
    val productosEnVenta: Flow<List<ProductoEnVenta>> = ventaDao.getAll()

    suspend fun addAlmacenItem(item: AlmacenItem) = almacenDao.insert(item)
    suspend fun updateAlmacenItem(item: AlmacenItem) = almacenDao.update(item)
    suspend fun deleteAlmacenItem(item: AlmacenItem) = almacenDao.delete(item)

    suspend fun addProductoEnVenta(item: ProductoEnVenta) = ventaDao.insert(item)
    suspend fun updateProductoEnVenta(item: ProductoEnVenta) = ventaDao.update(item)
    suspend fun deleteProductoEnVenta(item: ProductoEnVenta) = ventaDao.delete(item)
}
