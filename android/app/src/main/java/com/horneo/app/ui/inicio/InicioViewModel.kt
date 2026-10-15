package com.horneo.app.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.horneo.app.HorneoApplication
import com.horneo.app.data.remote.mensajeDeError
import com.horneo.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InicioViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _resultado = MutableStateFlow<String?>(null)
    val resultado: StateFlow<String?> = _resultado.asStateFlow()

    fun comprobarConexion() {
        viewModelScope.launch {
            _resultado.value = authRepository.comprobarSesion().fold(
                onSuccess = { "El servidor te reconoce como ${it.nombre} (${it.rol})" },
                onFailure = { mensajeDeError(it) }
            )
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.logout() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                InicioViewModel(app.container.authRepository)
            }
        }
    }
}
