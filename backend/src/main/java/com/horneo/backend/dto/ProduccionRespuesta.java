package com.horneo.backend.dto;

import java.time.LocalDateTime;

import com.horneo.backend.entity.Produccion;

public record ProduccionRespuesta(
		Long id,
		LocalDateTime fecha,
		Long productoId,
		String producto,
		int cantidad,
		String usuario) {

	public static ProduccionRespuesta de(Produccion produccion) {
		return new ProduccionRespuesta(
				produccion.getId(),
				produccion.getFecha(),
				produccion.getProducto().getId(),
				produccion.getProducto().getNombre(),
				produccion.getCantidad(),
				produccion.getUsuario().getNombre());
	}
}
