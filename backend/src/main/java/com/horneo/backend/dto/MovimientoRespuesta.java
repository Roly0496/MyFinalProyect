package com.horneo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.horneo.backend.entity.Movimiento;
import com.horneo.backend.entity.TipoMovimiento;

public record MovimientoRespuesta(
		Long id,
		LocalDateTime fecha,
		TipoMovimiento tipo,
		Long materiaPrimaId,
		Long productoId,
		String elemento,
		BigDecimal cantidad,
		String motivo,
		String usuario,
		Long produccionId,
		Long ventaId) {

	public static MovimientoRespuesta de(Movimiento movimiento) {
		boolean esMateriaPrima = movimiento.getMateriaPrima() != null;
		return new MovimientoRespuesta(
				movimiento.getId(),
				movimiento.getFecha(),
				movimiento.getTipo(),
				esMateriaPrima ? movimiento.getMateriaPrima().getId() : null,
				esMateriaPrima ? null : movimiento.getProducto().getId(),
				esMateriaPrima ? movimiento.getMateriaPrima().getNombre() : movimiento.getProducto().getNombre(),
				movimiento.getCantidad(),
				movimiento.getMotivo(),
				movimiento.getUsuario().getNombre(),
				movimiento.getProduccion() == null ? null : movimiento.getProduccion().getId(),
				movimiento.getVenta() == null ? null : movimiento.getVenta().getId());
	}
}
