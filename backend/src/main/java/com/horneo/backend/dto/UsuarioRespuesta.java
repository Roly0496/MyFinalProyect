package com.horneo.backend.dto;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;

public record UsuarioRespuesta(Long id, String nombre, String username, Rol rol, boolean activo) {

	public static UsuarioRespuesta de(Usuario usuario) {
		return new UsuarioRespuesta(usuario.getId(), usuario.getNombre(), usuario.getUsername(), usuario.getRol(),
				usuario.isActivo());
	}
}
