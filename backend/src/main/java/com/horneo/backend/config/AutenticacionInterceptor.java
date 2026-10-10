package com.horneo.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.exception.AccesoDenegadoException;
import com.horneo.backend.exception.NoAutenticadoException;
import com.horneo.backend.repository.UsuarioRepository;
import com.horneo.backend.service.TokenService;

@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

	public static final String ATRIBUTO_USUARIO = "usuario";
	private static final String PREFIJO = "Bearer ";

	private final TokenService tokenService;
	private final UsuarioRepository usuarioRepository;

	public AutenticacionInterceptor(TokenService tokenService, UsuarioRepository usuarioRepository) {
		this.tokenService = tokenService;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod metodo)) {
			return true;
		}

		String cabecera = request.getHeader("Authorization");
		if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
			throw new NoAutenticadoException("Hay que iniciar sesión");
		}
		Long usuarioId = tokenService.obtenerUsuarioId(cabecera.substring(PREFIJO.length()));

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoAutenticadoException("El usuario no existe o está desactivado"));

		if (metodo.hasMethodAnnotation(SoloAdmin.class) && usuario.getRol() != Rol.ADMIN) {
			throw new AccesoDenegadoException();
		}

		request.setAttribute(ATRIBUTO_USUARIO, usuario);
		return true;
	}
}
