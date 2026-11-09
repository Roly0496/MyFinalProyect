package com.horneo.app.ui

import com.horneo.app.data.remote.Unidad
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Test
    fun losPreciosLlevanSiempreDosDecimales() {
        assertEquals("1,20 €", formatearPrecio(BigDecimal("1.20")))
        assertEquals("15,00 €", formatearPrecio(BigDecimal("15")))
        assertEquals("1.250,50 €", formatearPrecio(BigDecimal("1250.5")))
    }

    @Test
    fun lasCantidadesDeLosMovimientosLlevanSigno() {
        assertEquals("+1.000 g", formatearCantidadConSigno(BigDecimal("1000.00"), Unidad.GRAMO))
        assertEquals("-150 g", formatearCantidadConSigno(BigDecimal("-150.00"), Unidad.GRAMO))
        assertEquals("0 ud", formatearCantidadConSigno(BigDecimal("0.00"), Unidad.UNIDAD))
    }

    @Test
    fun elPrecioPorGramoSeMuestraPorKilo() {
        assertEquals("9,00 €/kg", formatearPrecioUnitario(BigDecimal("0.009000"), Unidad.GRAMO))
        assertEquals("1,10 €/l", formatearPrecioUnitario(BigDecimal("0.001100"), Unidad.MILILITRO))
        assertEquals("0,20 €/ud", formatearPrecioUnitario(BigDecimal("0.200000"), Unidad.UNIDAD))
    }

    @Test
    fun laFechaSeMuestraConDiaMesYAno() {
        assertEquals("09/11/2026 07:05", formatearFechaHora(LocalDateTime.of(2026, 11, 9, 7, 5, 30)))
    }

    @Test
    fun elDiaSeNombraConHoyAyerOElDiaDeLaSemana() {
        val hoy = LocalDate.of(2026, 11, 9)  // lunes

        assertEquals("Hoy, 9 nov", formatearDia(hoy, hoy))
        assertEquals("Ayer, 8 nov", formatearDia(LocalDate.of(2026, 11, 8), hoy))
        assertEquals("lun, 2 nov", formatearDia(LocalDate.of(2026, 11, 2), hoy))
        // De otro año: se añade el año
        assertEquals("mié, 31 dic 2025", formatearDia(LocalDate.of(2025, 12, 31), hoy))
    }

    @Test
    fun laHoraSeMuestraSinSegundos() {
        assertEquals("07:40", formatearHora(LocalDateTime.of(2026, 11, 9, 7, 40, 59)))
    }
}
