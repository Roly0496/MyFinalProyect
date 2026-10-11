package com.horneo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.horneo.backend.entity.Rol;

public record ActualizarUsuarioPeticion(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
		String nombre,

		@NotNull(message = "El rol es obligatorio")
		Rol rol) {
}
