package com.horneo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.horneo.app.R

// Atkinson Hyperlegible Next (Braille Institute): cada letra y cada número tienen una forma
// distinta (0/O, 1/l/I), lo que evita errores al leer cantidades. Licencia SIL OFL 1.1
// (copia en assets/licencias). Se usan tres archivos estáticos generados a partir de la fuente
// variable, porque el grosor de una fuente variable solo se aplica desde Android 8 (API 26).
val AtkinsonHyperlegibleNext = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)

// Cambia solo la fuente de un estilo y activa las cifras de ancho fijo ("tnum"),
// para que las cantidades queden alineadas en columnas.
private fun TextStyle.conAtkinson() = copy(
    fontFamily = AtkinsonHyperlegibleNext,
    fontFeatureSettings = "tnum",
)

// Se mantienen los tamaños de Material 3 (en sp, que crecen con el tamaño de letra del sistema).
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.conAtkinson(),
    displayMedium = base.displayMedium.conAtkinson(),
    displaySmall = base.displaySmall.conAtkinson(),
    headlineLarge = base.headlineLarge.conAtkinson(),
    headlineMedium = base.headlineMedium.conAtkinson(),
    headlineSmall = base.headlineSmall.conAtkinson(),
    titleLarge = base.titleLarge.conAtkinson(),
    titleMedium = base.titleMedium.conAtkinson(),
    titleSmall = base.titleSmall.conAtkinson(),
    bodyLarge = base.bodyLarge.conAtkinson(),
    bodyMedium = base.bodyMedium.conAtkinson(),
    bodySmall = base.bodySmall.conAtkinson(),
    labelLarge = base.labelLarge.conAtkinson(),
    labelMedium = base.labelMedium.conAtkinson(),
    labelSmall = base.labelSmall.conAtkinson(),
)
