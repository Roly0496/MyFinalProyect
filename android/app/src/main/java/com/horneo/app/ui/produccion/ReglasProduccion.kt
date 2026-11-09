package com.horneo.app.ui.produccion

import com.horneo.app.data.remote.IngredienteRespuesta
import com.horneo.app.data.remote.Unidad
import java.math.BigDecimal
import java.math.RoundingMode

// Cuentas de la vista previa de una producción. Están aparte, en funciones normales de Kotlin,
// para poder probarlas con pruebas unitarias sin emulador (igual que las reglas de movimientos).
// Son las mismas cuentas que hace el backend al registrar: la app las repite para avisar antes de
// enviar, pero quien decide de verdad es el backend, que tiene el stock real en ese momento.

// Lo que se gastará de una materia prima, comparado con lo que hay.
data class ConsumoIngrediente(
    val materiaPrimaId: Long,
    val nombre: String,
    val unidad: Unidad,
    val necesario: BigDecimal,
    val disponible: BigDecimal,
) {
    // Cuánto falta (0 si alcanza).
    val falta: BigDecimal get() = (necesario - disponible).max(BigDecimal.ZERO)
    val alcanza: Boolean get() = falta.signum() == 0
}

// Receta (por unidad) × cantidad, comparada con el stock de cada materia prima.
// "stocks" relaciona el id de cada materia prima con su stock actual. Si una no aparece
// (por ejemplo, porque se desactivó), se cuenta como stock 0.
fun calcularConsumo(
    receta: List<IngredienteRespuesta>,
    cantidad: Int,
    stocks: Map<Long, BigDecimal>,
): List<ConsumoIngrediente> = receta.map { ingrediente ->
    ConsumoIngrediente(
        materiaPrimaId = ingrediente.materiaPrimaId,
        nombre = ingrediente.materiaPrimaNombre,
        unidad = ingrediente.unidad,
        necesario = ingrediente.cantidad.multiply(BigDecimal(cantidad)),
        disponible = stocks[ingrediente.materiaPrimaId] ?: BigDecimal.ZERO,
    )
}

// Cuántas unidades se pueden fabricar como máximo con el stock actual: para cada ingrediente,
// stock ÷ cantidad por unidad, redondeado hacia abajo (no se fabrica media unidad); el máximo es
// el menor de todos, porque el ingrediente que menos alcanza es el que manda.
fun maximoProducible(receta: List<IngredienteRespuesta>, stocks: Map<Long, BigDecimal>): Int {
    val porIngrediente = receta
        .filter { it.cantidad.signum() > 0 }
        .map { ingrediente ->
            val stock = stocks[ingrediente.materiaPrimaId] ?: BigDecimal.ZERO
            stock.divide(ingrediente.cantidad, 0, RoundingMode.DOWN).toInt()
        }
    // Sin receta no se puede producir nada.
    return porIngrediente.minOrNull() ?: 0
}
