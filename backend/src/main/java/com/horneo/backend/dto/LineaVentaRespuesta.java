package com.horneo.backend.dto;

import java.math.BigDecimal;

import com.horneo.backend.entity.DetalleVenta;

public record LineaVentaRespuesta(
		Long productoId,
		String producto,
		int cantidad,
		BigDecimal precioUnitario,
		BigDecimal subtotal) {

	public static LineaVentaRespuesta de(DetalleVenta detalle) {
		return new LineaVentaRespuesta(
				detalle.getProducto().getId(),
				detalle.getProducto().getNombre(),
				detalle.getCantidad(),
				detalle.getPrecioUnitario(),
				detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
	}
}
