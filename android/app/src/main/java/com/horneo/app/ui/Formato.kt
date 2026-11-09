package com.horneo.app.ui

import com.horneo.app.data.remote.Unidad
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Formato español: punto para los miles, coma para los decimales y sin ceros sobrantes
// (12500.00 → "12.500", 0.10 → "0,1"). Como mucho dos decimales, igual que en la base de datos.
private val formatoCantidad = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.forLanguageTag("es-ES")))

fun formatearCantidad(cantidad: BigDecimal, unidad: Unidad): String =
    "${formatoCantidad.format(cantidad)} ${unidad.simbolo}"

// Precios: siempre con dos decimales, como se escriben en euros (1.20 → "1,20 €", 15 → "15,00 €").
private val formatoPrecio = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("es-ES")))

fun formatearPrecio(precio: BigDecimal): String = "${formatoPrecio.format(precio)} €"
