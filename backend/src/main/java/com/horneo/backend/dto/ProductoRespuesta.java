package com.horneo.backend.dto;

import java.math.BigDecimal;

import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoProducto;

public record ProductoRespuesta(
		Long id,
		String nombre,
		TipoProducto tipo,
		BigDecimal precioVenta,
		BigDecimal precioCompra,
		int stockActual,
		boolean activo) {

	public static ProductoRespuesta de(Producto producto) {
		return new ProductoRespuesta(
				producto.getId(),
				producto.getNombre(),
				producto.getTipo(),
				producto.getPrecioVenta(),
				producto.getPrecioCompra(),
				producto.getStockActual(),
				producto.isActivo());
	}
}
