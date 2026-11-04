package com.horneo.app.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.horneo.app.R

// Logo de Horneo (monograma H), dibujado con las mismas capas que el icono de la app.
// Las capas miden 108 dp pero el launcher solo enseña los 72 dp centrales: por eso se dibujan
// a 108/72 del tamaño del logo y la forma redondeada recorta lo que sobra por los bordes.
// Es decorativo (contentDescription = null): siempre va junto al nombre «Horneo» en texto.
@Composable
fun LogoHorneo(
    modifier: Modifier = Modifier,
    tamano: Dp = 80.dp,
) {
    val tamanoCapa = tamano * 108f / 72f

    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.24f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.requiredSize(tamanoCapa)
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(tamanoCapa)
        )
    }
}
