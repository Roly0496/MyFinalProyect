package com.horneo.backend.dto;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;

public record LoginRespuesta(String token, Long id, String nombre, Rol rol) {

	public static LoginRespuesta de(String token, Usuario usuario) {
		return new LoginRespuesta(token, usuario.getId(), usuario.getNombre(), usuario.getRol());
	}
}
