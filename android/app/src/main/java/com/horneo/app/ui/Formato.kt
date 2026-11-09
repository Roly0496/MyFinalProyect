package com.horneo.app.ui

import com.horneo.app.data.remote.Unidad
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formato español: punto para los miles, coma para los decimales y sin ceros sobrantes
// (12500.00 → "12.500", 0.10 → "0,1"). Como mucho dos decimales, igual que en la base de datos.
private val formatoCantidad = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.forLanguageTag("es-ES")))

fun formatearCantidad(cantidad: BigDecimal, unidad: Unidad): String =
    "${formatoCantidad.format(cantidad)} ${unidad.simbolo}"

// Precios: siempre con dos decimales, como se escriben en euros (1.20 → "1,20 €", 15 → "15,00 €").
private val formatoPrecio = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("es-ES")))

fun formatearPrecio(precio: BigDecimal): String = "${formatoPrecio.format(precio)} €"

// Cantidades de los movimientos: siempre con signo, para que se lea si entra o sale stock
// (1000 → "+1.000 g", -150 → "-150 g"). El cero se muestra sin signo.
private val formatoCantidadConSigno =
    DecimalFormat("+#,##0.##;-#,##0.##", DecimalFormatSymbols(Locale.forLanguageTag("es-ES")))

fun formatearCantidadConSigno(cantidad: BigDecimal, unidad: Unidad): String =
    if (cantidad.signum() == 0) formatearCantidad(cantidad, unidad)
    else "${formatoCantidadConSigno.format(cantidad)} ${unidad.simbolo}"

// El precio se guarda por unidad base (euros por gramo o por mililitro), pero así es difícil de leer:
// 0,009 €/g se entiende mejor como 9,00 €/kg. Por eso gramos y mililitros se muestran por kilo y por litro.
fun formatearPrecioUnitario(precio: BigDecimal, unidad: Unidad): String = when (unidad) {
    Unidad.GRAMO -> "${formatearPrecio(precio.multiply(MIL))}/kg"
    Unidad.MILILITRO -> "${formatearPrecio(precio.multiply(MIL))}/l"
    Unidad.UNIDAD -> "${formatearPrecio(precio)}/ud"
}

private val MIL = BigDecimal(1000)

// Fecha y hora de los movimientos, en el formato habitual en España: "09/11/2026 09:39".
private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun formatearFechaHora(fecha: LocalDateTime): String = fecha.format(formatoFechaHora)
