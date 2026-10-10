package com.horneo.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record RegistrarVentaPeticion(
		@NotEmpty(message = "Una venta debe tener al menos un producto")
		List<@Valid LineaVentaPeticion> lineas) {
}
