package com.horneo.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimientoMateriaPrimaPeticion(
		@NotNull(message = "La materia prima es obligatoria")
		Long materiaPrimaId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor que 0")
		BigDecimal cantidad,

		@Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
		String motivo) {
}
