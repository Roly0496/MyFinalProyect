package com.horneo.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.horneo.app.data.remote.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Sesion(val token: String, val usuarioId: Long, val nombre: String, val rol: Rol)

private val Context.dataStore by preferencesDataStore(name = "sesion")

class SesionStore(private val context: Context) {

    private object Claves {
        val TOKEN = stringPreferencesKey("token")
        val USUARIO_ID = longPreferencesKey("usuario_id")
        val NOMBRE = stringPreferencesKey("nombre")
        val ROL = stringPreferencesKey("rol")
    }

    val sesion: Flow<Sesion?> = context.dataStore.data.map { datos ->
        val token = datos[Claves.TOKEN] ?: return@map null
        Sesion(
            token = token,
            usuarioId = datos[Claves.USUARIO_ID] ?: 0,
            nombre = datos[Claves.NOMBRE].orEmpty(),
            rol = Rol.valueOf(datos[Claves.ROL] ?: Rol.EMPLEADO.name)
        )
    }

    suspend fun token(): String? = sesion.first()?.token

    suspend fun guardar(sesion: Sesion) {
        context.dataStore.edit { datos ->
            datos[Claves.TOKEN] = sesion.token
            datos[Claves.USUARIO_ID] = sesion.usuarioId
            datos[Claves.NOMBRE] = sesion.nombre
            datos[Claves.ROL] = sesion.rol.name
        }
    }

    suspend fun borrar() {
        context.dataStore.edit { it.clear() }
    }
}
