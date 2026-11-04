package com.horneo.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HorneoApi {

    @POST("api/login")
    suspend fun login(@Body peticion: LoginPeticion): LoginRespuesta

    @GET("api/usuarios/yo")
    suspend fun yo(): UsuarioRespuesta

    @GET("api/materias-primas")
    suspend fun materiasPrimas(): List<MateriaPrimaRespuesta>
}
