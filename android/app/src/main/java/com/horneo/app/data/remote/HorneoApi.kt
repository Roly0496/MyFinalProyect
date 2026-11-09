package com.horneo.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HorneoApi {

    @POST("api/login")
    suspend fun login(@Body peticion: LoginPeticion): LoginRespuesta

    @GET("api/usuarios/yo")
    suspend fun yo(): UsuarioRespuesta

    @GET("api/materias-primas")
    suspend fun materiasPrimas(): List<MateriaPrimaRespuesta>

    // {id} se sustituye por el valor del parámetro marcado con @Path("id").
    @GET("api/materias-primas/{id}")
    suspend fun materiaPrima(@Path("id") id: Long): MateriaPrimaRespuesta

    @GET("api/materias-primas/{id}/movimientos")
    suspend fun movimientosDeMateriaPrima(@Path("id") id: Long): List<MovimientoRespuesta>

    @GET("api/productos")
    suspend fun productos(): List<ProductoRespuesta>

    @GET("api/productos/{id}")
    suspend fun producto(@Path("id") id: Long): ProductoRespuesta

    @GET("api/productos/{id}/receta")
    suspend fun recetaDeProducto(@Path("id") id: Long): List<IngredienteRespuesta>

    // Solo administrador: el backend responde 403 si lo pide un empleado.
    @GET("api/productos/{id}/coste")
    suspend fun costeDeProducto(@Path("id") id: Long): CosteRespuesta

    @GET("api/productos/{id}/movimientos")
    suspend fun movimientosDeProducto(@Path("id") id: Long): List<MovimientoRespuesta>

    // Movimientos de materias primas. Los tres devuelven el movimiento creado (201 Created).
    @POST("api/movimientos/entradas/materias-primas")
    suspend fun entradaMateriaPrima(@Body peticion: MovimientoMateriaPrimaPeticion): MovimientoRespuesta

    @POST("api/movimientos/mermas/materias-primas")
    suspend fun mermaMateriaPrima(@Body peticion: MovimientoMateriaPrimaPeticion): MovimientoRespuesta

    // Solo administrador: el backend responde 403 si lo intenta un empleado.
    @POST("api/movimientos/ajustes/materias-primas")
    suspend fun ajusteMateriaPrima(@Body peticion: AjusteMateriaPrimaPeticion): MovimientoRespuesta

    // Movimientos de productos. La entrada solo vale para productos de reventa.
    @POST("api/movimientos/entradas/productos")
    suspend fun entradaProducto(@Body peticion: MovimientoProductoPeticion): MovimientoRespuesta

    @POST("api/movimientos/mermas/productos")
    suspend fun mermaProducto(@Body peticion: MovimientoProductoPeticion): MovimientoRespuesta

    // Solo administrador.
    @POST("api/movimientos/ajustes/productos")
    suspend fun ajusteProducto(@Body peticion: AjusteProductoPeticion): MovimientoRespuesta

    // Producciones de un día. @Query añade el parámetro a la URL: api/producciones?fecha=2026-11-09
    @GET("api/producciones")
    suspend fun producciones(@Query("fecha") fecha: String): List<ProduccionRespuesta>

    @POST("api/producciones")
    suspend fun registrarProduccion(@Body peticion: RegistrarProduccionPeticion): ProduccionRespuesta

    @GET("api/ventas")
    suspend fun ventas(@Query("fecha") fecha: String): List<VentaRespuesta>

    @GET("api/ventas/{id}")
    suspend fun ticket(@Path("id") id: Long): TicketRespuesta

    // Solo administrador.
    @GET("api/ventas/caja")
    suspend fun caja(@Query("fecha") fecha: String): CajaRespuesta

    @POST("api/ventas")
    suspend fun registrarVenta(@Body peticion: RegistrarVentaPeticion): TicketRespuesta
}
