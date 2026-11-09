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
		return construir(materiaPrima, materiaPrima.getPrecioUnitario());
	}

	// Para el empleado, que no puede ver costes: el precio unitario va siempre a null.
	// Con la receta y estos precios se podría calcular el coste de cada producto.
	public static MateriaPrimaRespuesta sinCostes(MateriaPrima materiaPrima) {
		return construir(materiaPrima, null);
	}

	// Un solo sitio donde se calcula bajoMinimo, para que las dos versiones no se separen.
	private static MateriaPrimaRespuesta construir(MateriaPrima materiaPrima, BigDecimal precioUnitario) {
		return new MateriaPrimaRespuesta(
				materiaPrima.getId(),
				materiaPrima.getNombre(),
				materiaPrima.getUnidad(),
				materiaPrima.getStockActual(),
				materiaPrima.getStockMinimo(),
				precioUnitario,
				materiaPrima.isActivo(),
				materiaPrima.getStockActual().compareTo(materiaPrima.getStockMinimo()) < 0);
	}
}
