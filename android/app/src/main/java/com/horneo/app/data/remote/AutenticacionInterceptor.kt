package com.horneo.app.data.remote

import com.horneo.app.data.local.SesionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AutenticacionInterceptor(private val sesionStore: SesionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sesionStore.token() }
        val peticion = chain.request()
        if (token == null) {
            return chain.proceed(peticion)
        }
        return chain.proceed(
            peticion.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
