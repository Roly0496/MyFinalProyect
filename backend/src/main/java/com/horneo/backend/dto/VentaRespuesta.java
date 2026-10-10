package com.horneo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.horneo.backend.entity.Venta;

public record VentaRespuesta(Long id, LocalDateTime fecha, BigDecimal total, String usuario) {

	public static VentaRespuesta de(Venta venta) {
		return new VentaRespuesta(venta.getId(), venta.getFecha(), venta.getTotal(), venta.getUsuario().getNombre());
	}
}
