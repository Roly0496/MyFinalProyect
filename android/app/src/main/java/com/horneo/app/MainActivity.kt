package com.horneo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.horneo.app.data.local.Sesion
import com.horneo.app.ui.login.LoginScreen
import com.horneo.app.ui.navegacion.PantallaPrincipal
import com.horneo.app.ui.theme.HorneoTheme
import kotlinx.coroutines.flow.map

private sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class ConSesion(val sesion: Sesion) : EstadoSesion
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HorneoApplication).container
        setContent {
            HorneoTheme {
                HorneoApp(container)
            }
        }
    }
}

@Composable
private fun HorneoApp(container: AppContainer) {
    val estadoFlow = remember {
        container.authRepository.sesion.map { sesion ->
            if (sesion == null) EstadoSesion.SinSesion else EstadoSesion.ConSesion(sesion)
        }
    }
    val estado by estadoFlow.collectAsStateWithLifecycle(initialValue = EstadoSesion.Cargando)

    when (val actual = estado) {
        // PantallaPrincipal tiene su propio Scaffold (con la barra inferior), por eso va fuera del de abajo:
        // dos Scaffold anidados dejarían dos veces el hueco de las barras del sistema.
        is EstadoSesion.ConSesion -> PantallaPrincipal(actual.sesion)
        else -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            if (actual == EstadoSesion.Cargando) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LoginScreen(modifier)
            }
        }
    }
}
