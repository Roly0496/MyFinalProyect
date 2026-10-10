package com.horneo.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ActualizarMateriaPrimaPeticion(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
		String nombre,

		@NotNull(message = "El stock mínimo es obligatorio")
		@PositiveOrZero(message = "El stock mínimo no puede ser negativo")
		BigDecimal stockMinimo,

		@NotNull(message = "El precio unitario es obligatorio")
		@PositiveOrZero(message = "El precio unitario no puede ser negativo")
		BigDecimal precioUnitario) {
}
