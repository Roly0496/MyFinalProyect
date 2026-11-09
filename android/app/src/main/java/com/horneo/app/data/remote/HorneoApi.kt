package com.horneo.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

    // Movimientos de materias primas. Los tres devuelven el movimiento creado (201 Created).
    @POST("api/movimientos/entradas/materias-primas")
    suspend fun entradaMateriaPrima(@Body peticion: MovimientoMateriaPrimaPeticion): MovimientoRespuesta

    @POST("api/movimientos/mermas/materias-primas")
    suspend fun mermaMateriaPrima(@Body peticion: MovimientoMateriaPrimaPeticion): MovimientoRespuesta

    // Solo administrador: el backend responde 403 si lo intenta un empleado.
    @POST("api/movimientos/ajustes/materias-primas")
    suspend fun ajusteMateriaPrima(@Body peticion: AjusteMateriaPrimaPeticion): MovimientoRespuesta
}
