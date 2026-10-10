package com.horneo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LineaVentaPeticion(
		@NotNull(message = "El producto es obligatorio")
		Long productoId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor que 0")
		Integer cantidad) {
}
