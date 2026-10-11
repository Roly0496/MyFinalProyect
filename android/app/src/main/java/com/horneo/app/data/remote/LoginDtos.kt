package com.horneo.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
enum class Rol { ADMIN, EMPLEADO }

@Serializable
data class LoginPeticion(val username: String, val password: String)

@Serializable
data class LoginRespuesta(val token: String, val id: Long, val nombre: String, val rol: Rol)
