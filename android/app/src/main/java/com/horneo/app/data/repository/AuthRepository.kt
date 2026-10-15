package com.horneo.app.data.repository

import com.horneo.app.data.local.Sesion
import com.horneo.app.data.local.SesionStore
import com.horneo.app.data.remote.HorneoApi
import com.horneo.app.data.remote.LoginPeticion
import com.horneo.app.data.remote.UsuarioRespuesta
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val api: HorneoApi, private val sesionStore: SesionStore) {

    val sesion: Flow<Sesion?> = sesionStore.sesion

    suspend fun login(username: String, password: String): Result<Sesion> = runCatching {
        val respuesta = api.login(LoginPeticion(username.trim(), password))
        val sesion = Sesion(respuesta.token, respuesta.id, respuesta.nombre, respuesta.rol)
        sesionStore.guardar(sesion)
        sesion
    }

    suspend fun comprobarSesion(): Result<UsuarioRespuesta> = runCatching { api.yo() }

    suspend fun logout() {
        sesionStore.borrar()
    }
}
