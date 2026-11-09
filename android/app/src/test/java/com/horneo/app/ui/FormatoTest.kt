package com.horneo.app.ui

import com.horneo.app.data.remote.Unidad
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FormatoTest {

    @Test
    fun usaPuntoParaLosMiles() {
        assertEquals("12.500 g", formatearCantidad(BigDecimal("12500.00"), Unidad.GRAMO))
    }

    @Test
    fun quitaLosCerosSobrantes() {
        assertEquals("850 g", formatearCantidad(BigDecimal("850.00"), Unidad.GRAMO))
        assertEquals("0,1 ud", formatearCantidad(BigDecimal("0.10"), Unidad.UNIDAD))
    }

    @Test
    fun usaComaParaLosDecimales() {
        assertEquals("1.234,56 ml", formatearCantidad(BigDecimal("1234.56"), Unidad.MILILITRO))
    }
}
