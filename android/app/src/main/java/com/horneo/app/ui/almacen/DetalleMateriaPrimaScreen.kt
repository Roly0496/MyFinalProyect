package com.horneo.app.ui.almacen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.horneo.app.R
import com.horneo.app.ui.componentes.PantallaProvisional

// De momento solo la barra superior con el nombre y la flecha para volver.
// El stock, el historial y los botones de movimientos se añaden en el siguiente paso.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMateriaPrimaScreen(
    nombre: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(nombre) },
            navigationIcon = {
                // Es un botón, así que lleva descripción para TalkBack.
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                }
            }
        )
        PantallaProvisional("Detalle")
    }
}
