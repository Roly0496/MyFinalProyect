package com.horneo.backend.dto;

import java.math.BigDecimal;

import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Unidad;

public record MateriaPrimaRespuesta(
		Long id,
		String nombre,
		Unidad unidad,
		BigDecimal stockActual,
		BigDecimal stockMinimo,
		BigDecimal precioUnitario,
		boolean activo,
		boolean bajoMinimo) {

	public static MateriaPrimaRespuesta de(MateriaPrima materiaPrima) {
		return new MateriaPrimaRespuesta(
				materiaPrima.getId(),
				materiaPrima.getNombre(),
				materiaPrima.getUnidad(),
				materiaPrima.getStockActual(),
				materiaPrima.getStockMinimo(),
				materiaPrima.getPrecioUnitario(),
				materiaPrima.isActivo(),
				materiaPrima.getStockActual().compareTo(materiaPrima.getStockMinimo()) < 0);
	}
}
