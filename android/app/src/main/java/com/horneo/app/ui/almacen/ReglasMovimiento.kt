package com.horneo.app.ui.almacen

import com.horneo.app.data.remote.Unidad
import com.horneo.app.ui.formatearCantidad
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// Reglas del formulario de movimientos. Están aparte, en funciones normales de Kotlin (sin Compose
// ni Android), para poder probarlas con pruebas unitarias rápidas, sin emulador.

// Los tres formularios comparten pantalla: cada tipo dice sus textos y si el motivo es obligatorio.
// @Serializable porque el tipo viaja dentro de la ruta de navegación.
@Serializable
enum class TipoFormularioMovimiento(
    val titulo: String,
    val etiquetaCantidad: String,
    val ayudaCantidad: String,
    val motivoObligatorio: Boolean,
    val textoBoton: String,
) {
    ENTRADA("Entrada", "Cantidad que entra", "Se sumará al stock.", false, "Guardar entrada"),
    MERMA("Merma", "Cantidad perdida", "Escríbela en positivo. Se restará del stock.", true, "Guardar merma"),
    AJUSTE("Ajuste", "Stock real contado", "El stock pasará a ser esta cantidad.", true, "Guardar ajuste"),
}

// Lee un número escrito por el usuario. Acepta coma o punto como separador decimal, porque en España
// se escribe con coma pero algunos teclados solo muestran el punto. Devuelve null si no es un número.
// No acepta signos ni separador de miles: "1.000" se lee como 1,000 (tres decimales) y lo rechaza
// la regla de los dos decimales, en lugar de guardar 1 cuando el usuario quería decir mil.
fun leerCantidad(texto: String): BigDecimal? {
    val limpio = texto.trim().replace(',', '.')
    if (!Regex("""\d+(\.\d*)?|\.\d+""").matches(limpio)) return null
    return BigDecimal(limpio)
}

data class ResultadoValidacion(
    val cantidad: BigDecimal?,
    val errorCantidad: String?,
    val errorMotivo: String?,
) {
    val esValido: Boolean get() = errorCantidad == null && errorMotivo == null
}

// Comprueba el formulario antes de enviarlo. Son las mismas reglas que aplica el backend: la app las
// repite para avisar al momento y al lado del campo, pero quien decide de verdad es el backend.
fun validarMovimiento(
    tipo: TipoFormularioMovimiento,
    textoCantidad: String,
    motivo: String,
    stockActual: BigDecimal,
    unidad: Unidad,
): ResultadoValidacion {
    val cantidad = leerCantidad(textoCantidad)
    val errorCantidad = when {
        textoCantidad.isBlank() -> "Escribe la cantidad"
        cantidad == null -> "Escribe solo números, por ejemplo 1,5"
        // En la base de datos las cantidades tienen dos decimales.
        cantidad.scale() > 2 -> "Como mucho dos decimales"
        tipo != TipoFormularioMovimiento.AJUSTE && cantidad.signum() == 0 -> "La cantidad debe ser mayor que 0"
        // El stock nunca puede quedar en negativo.
        tipo == TipoFormularioMovimiento.MERMA && cantidad > stockActual ->
            "No puede ser mayor que el stock actual (${formatearCantidad(stockActual, unidad)})"
        // Un ajuste que no cambia nada lo rechazaría el backend.
        tipo == TipoFormularioMovimiento.AJUSTE && cantidad.compareTo(stockActual) == 0 ->
            "El stock ya es ${formatearCantidad(stockActual, unidad)}"
        else -> null
    }
    val errorMotivo = when {
        tipo.motivoObligatorio && motivo.isBlank() -> "El motivo es obligatorio"
        motivo.trim().length > 255 -> "Como mucho 255 caracteres"
        else -> null
    }
    return ResultadoValidacion(if (errorCantidad == null) cantidad else null, errorCantidad, errorMotivo)
}

// Cómo quedará el stock al guardar, para enseñarlo antes de enviar.
fun stockDespues(tipo: TipoFormularioMovimiento, cantidad: BigDecimal, stockActual: BigDecimal): BigDecimal =
    when (tipo) {
        TipoFormularioMovimiento.ENTRADA -> stockActual + cantidad
        TipoFormularioMovimiento.MERMA -> stockActual - cantidad
        TipoFormularioMovimiento.AJUSTE -> cantidad
    }
