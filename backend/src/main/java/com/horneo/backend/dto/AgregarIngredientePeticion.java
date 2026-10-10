package com.horneo.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AgregarIngredientePeticion(
		@NotNull(message = "La materia prima es obligatoria")
		Long materiaPrimaId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor que 0")
		BigDecimal cantidad) {
}
