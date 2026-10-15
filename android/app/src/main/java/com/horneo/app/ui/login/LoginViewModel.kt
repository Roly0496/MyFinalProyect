package com.horneo.app.ui.login

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
) {
    val puedeEntrar: Boolean get() = username.isNotBlank() && password.isNotBlank() && !cargando
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarUsername(username: String) {
        _estado.update { it.copy(username = username, error = null) }
    }

    fun cambiarPassword(password: String) {
        _estado.update { it.copy(password = password, error = null) }
    }

    fun entrar() {
        val actual = _estado.value
        if (!actual.puedeEntrar) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            authRepository.login(actual.username, actual.password)
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = mensajeDeError(error)) } }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HorneoApplication
                LoginViewModel(app.container.authRepository)
            }
        }
    }
}
