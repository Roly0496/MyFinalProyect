package com.horneo.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AjusteMateriaPrimaPeticion(
		@NotNull(message = "La materia prima es obligatoria")
		Long materiaPrimaId,

		@NotNull(message = "El stock real es obligatorio")
		@PositiveOrZero(message = "El stock real no puede ser negativo")
		BigDecimal stockReal,

		@NotBlank(message = "El motivo es obligatorio")
		@Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
		String motivo) {
}
