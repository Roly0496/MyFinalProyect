package com.horneo.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class ProblemDetail(val detail: String? = null, val errores: Map<String, String> = emptyMap())

private val json = Json { ignoreUnknownKeys = true }

fun mensajeDeError(error: Throwable): String = when (error) {
    is HttpException -> {
        val cuerpo = error.response()?.errorBody()?.string()
        val problema = cuerpo?.let { runCatching { json.decodeFromString<ProblemDetail>(it) }.getOrNull() }
        problema?.errores?.values?.firstOrNull()
            ?: problema?.detail
            ?: "Error del servidor (${error.code()})"
    }
    is IOException -> "No se puede conectar con el servidor"
    else -> error.message ?: "Error inesperado"
}
