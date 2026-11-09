package com.horneo.app.ui.produccion

import com.horneo.app.data.remote.IngredienteRespuesta
import com.horneo.app.data.remote.Unidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ReglasProduccionTest {

    // Receta del croissant por unidad, como en el boceto
    private val harina = IngredienteRespuesta(1, 10, "Harina", Unidad.GRAMO, BigDecimal("50.00"))
    private val mantequilla = IngredienteRespuesta(2, 11, "Mantequilla", Unidad.GRAMO, BigDecimal("25.00"))
    private val huevos = IngredienteRespuesta(3, 12, "Huevos", Unidad.UNIDAD, BigDecimal("0.10"))
    private val receta = listOf(harina, mantequilla, huevos)

    private val stocks = mapOf(
        10L to BigDecimal("12500.00"),
        11L to BigDecimal("850.00"),
        12L to BigDecimal("10.00"),
    )

    @Test
    fun multiplicaLaRecetaPorLaCantidad() {
        val consumo = calcularConsumo(receta, 40, stocks)

        assertEquals(BigDecimal("2000.00"), consumo[0].necesario)
        assertEquals(BigDecimal("1000.00"), consumo[1].necesario)
        assertEquals(BigDecimal("4.00"), consumo[2].necesario)
    }

    @Test
    fun marcaLoQueFaltaYLoQueAlcanza() {
        val consumo = calcularConsumo(receta, 40, stocks)

        assertTrue(consumo[0].alcanza)
        // Mantequilla: hacen falta 1000 g y hay 850 g
        assertFalse(consumo[1].alcanza)
        assertEquals(BigDecimal("150.00"), consumo[1].falta)
    }

    @Test
    fun elMaximoLoMarcaElIngredienteQueMenosAlcanza() {
        // Harina 12500/50 = 250, mantequilla 850/25 = 34, huevos 10/0,1 = 100 → manda la mantequilla
        assertEquals(34, maximoProducible(receta, stocks))
    }

    @Test
    fun elMaximoSeRedondeaHaciaAbajo() {
        // 860 g / 25 g = 34,4 → 34 (no se fabrica media unidad)
        val conMasMantequilla = stocks + (11L to BigDecimal("860.00"))

        assertEquals(34, maximoProducible(receta, conMasMantequilla))
    }

    @Test
    fun unaMateriaPrimaQueNoApareceCuentaComoStockCero() {
        val sinHuevos = stocks - 12L

        assertEquals(0, maximoProducible(receta, sinHuevos))
        assertEquals(BigDecimal("4.00"), calcularConsumo(receta, 40, sinHuevos)[2].falta)
    }

    @Test
    fun sinRecetaNoSePuedeProducir() {
        assertEquals(0, maximoProducible(emptyList(), stocks))
    }
}
