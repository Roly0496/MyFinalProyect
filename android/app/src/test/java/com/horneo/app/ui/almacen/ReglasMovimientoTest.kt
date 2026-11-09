package com.horneo.app.ui.almacen

import com.horneo.app.data.remote.Unidad
import com.horneo.app.ui.almacen.TipoFormularioMovimiento.AJUSTE
import com.horneo.app.ui.almacen.TipoFormularioMovimiento.ENTRADA
import com.horneo.app.ui.almacen.TipoFormularioMovimiento.MERMA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ReglasMovimientoTest {

    private val stock = BigDecimal("850.00")

    private fun validar(tipo: TipoFormularioMovimiento, cantidad: String, motivo: String = "") =
        validarMovimiento(tipo, cantidad, motivo, stock, Unidad.GRAMO)

    @Test
    fun aceptaComaYPuntoComoSeparadorDecimal() {
        assertEquals(BigDecimal("1.5"), leerCantidad("1,5"))
        assertEquals(BigDecimal("1.5"), leerCantidad(" 1.5 "))
        assertEquals(BigDecimal("150"), leerCantidad("150"))
    }

    @Test
    fun rechazaLoQueNoEsUnNumero() {
        assertNull(leerCantidad("abc"))
        assertNull(leerCantidad("-5"))
        assertNull(leerCantidad("1,5,0"))
        assertNull(leerCantidad(""))
    }

    @Test
    fun noConfundeElSeparadorDeMilesConDecimales() {
        // "1.000" no se guarda como 1: tiene tres decimales y se rechaza
        assertEquals("Como mucho dos decimales", validar(ENTRADA, "1.000").errorCantidad)
    }

    @Test
    fun unaEntradaValidaNoNecesitaMotivo() {
        val resultado = validar(ENTRADA, "1000")

        assertTrue(resultado.esValido)
        assertEquals(BigDecimal("1000"), resultado.cantidad)
    }

    @Test
    fun laCantidadDebeSerMayorQueCero() {
        assertEquals("La cantidad debe ser mayor que 0", validar(ENTRADA, "0").errorCantidad)
        assertEquals("Escribe la cantidad", validar(ENTRADA, "  ").errorCantidad)
    }

    @Test
    fun laMermaExigeMotivoYNoPuedeSuperarElStock() {
        assertEquals("El motivo es obligatorio", validar(MERMA, "150", motivo = " ").errorMotivo)
        assertEquals(
            "No puede ser mayor que el stock actual (850 g)",
            validar(MERMA, "900", motivo = "Caducada").errorCantidad
        )
        assertTrue(validar(MERMA, "850", motivo = "Caducada").esValido)
    }

    @Test
    fun elAjustePermiteCeroPeroNoElMismoStock() {
        assertTrue(validar(AJUSTE, "0", motivo = "Recuento").esValido)
        assertEquals("El stock ya es 850 g", validar(AJUSTE, "850", motivo = "Recuento").errorCantidad)
    }

    @Test
    fun conErroresNoDevuelveCantidad() {
        val resultado = validar(MERMA, "900", motivo = "Caducada")

        assertFalse(resultado.esValido)
        assertNull(resultado.cantidad)
    }

    @Test
    fun calculaComoQuedaraElStock() {
        assertEquals(BigDecimal("1850.00"), stockDespues(ENTRADA, BigDecimal("1000"), stock))
        assertEquals(BigDecimal("700.00"), stockDespues(MERMA, BigDecimal("150"), stock))
        assertEquals(BigDecimal("600"), stockDespues(AJUSTE, BigDecimal("600"), stock))
    }
}
