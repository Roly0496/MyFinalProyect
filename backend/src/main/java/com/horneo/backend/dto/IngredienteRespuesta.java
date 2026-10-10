package com.horneo.backend.dto;

import java.math.BigDecimal;

import com.horneo.backend.entity.Ingrediente;
import com.horneo.backend.entity.Unidad;

public record IngredienteRespuesta(
		Long id,
		Long materiaPrimaId,
		String materiaPrimaNombre,
		Unidad unidad,
		BigDecimal cantidad) {

	public static IngredienteRespuesta de(Ingrediente ingrediente) {
		return new IngredienteRespuesta(
				ingrediente.getId(),
				ingrediente.getMateriaPrima().getId(),
				ingrediente.getMateriaPrima().getNombre(),
				ingrediente.getMateriaPrima().getUnidad(),
				ingrediente.getCantidad());
	}
}
