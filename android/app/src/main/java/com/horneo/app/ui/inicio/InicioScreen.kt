package com.horneo.app.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horneo.app.data.local.Sesion

@Composable
fun InicioScreen(
    sesion: Sesion,
    modifier: Modifier = Modifier,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)
) {
    val resultado by viewModel.resultado.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hola, ${sesion.nombre}", style = MaterialTheme.typography.headlineMedium)
        Text("Rol: ${sesion.rol}")

        Button(onClick = viewModel::comprobarConexion) {
            Text("Comprobar conexión")
        }
        resultado?.let { Text(it) }

        OutlinedButton(onClick = viewModel::cerrarSesion) {
            Text("Cerrar sesión")
        }
    }
}
