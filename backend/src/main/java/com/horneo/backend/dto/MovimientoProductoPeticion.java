package com.horneo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimientoProductoPeticion(
		@NotNull(message = "El producto es obligatorio")
		Long productoId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor que 0")
		Integer cantidad,

		@Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
		String motivo) {
}
