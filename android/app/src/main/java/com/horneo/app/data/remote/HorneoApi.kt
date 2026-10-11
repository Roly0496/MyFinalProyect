package com.horneo.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface HorneoApi {

    @POST("api/login")
    suspend fun login(@Body peticion: LoginPeticion): LoginRespuesta
}
