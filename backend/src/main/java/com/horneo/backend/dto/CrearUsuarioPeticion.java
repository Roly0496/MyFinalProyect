package com.horneo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.horneo.backend.entity.Rol;

public record CrearUsuarioPeticion(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
		String nombre,

		@NotBlank(message = "El nombre de usuario es obligatorio")
		@Size(max = 50, message = "El nombre de usuario no puede tener más de 50 caracteres")
		String username,

		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
		String password,

		@NotNull(message = "El rol es obligatorio")
		Rol rol) {
}
