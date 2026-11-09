package com.horneo.app

import android.content.Context
import com.horneo.app.data.local.SesionStore
import com.horneo.app.data.remote.AutenticacionInterceptor
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.repository.AuthRepository
import com.horneo.app.data.repository.MateriaPrimaRepository
import com.horneo.app.data.repository.ProductoRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {

    private val urlServidor = "http://10.0.2.2:8080/"

    private val json = Json { ignoreUnknownKeys = true }

    val sesionStore = SesionStore(context)

    private val clienteHttp = OkHttpClient.Builder()
        .addInterceptor(AutenticacionInterceptor(sesionStore))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val api: HorneoApi = Retrofit.Builder()
        .baseUrl(urlServidor)
        .client(clienteHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(HorneoApi::class.java)

    val authRepository = AuthRepository(api, sesionStore)

    val materiaPrimaRepository = MateriaPrimaRepository(api)

    val productoRepository = ProductoRepository(api)
}
