package com.horneo.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginPeticion(
		@NotBlank(message = "El usuario es obligatorio")
		String username,

		@NotBlank(message = "La contraseña es obligatoria")
		String password) {
}
