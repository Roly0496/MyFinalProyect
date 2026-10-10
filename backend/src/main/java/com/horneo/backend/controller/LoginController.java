package com.horneo.backend.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.dto.LoginPeticion;
import com.horneo.backend.dto.LoginRespuesta;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.TokenService;
import com.horneo.backend.service.UsuarioService;

@RestController
@RequestMapping("/api/login")
public class LoginController {

	private final UsuarioService usuarioService;
	private final TokenService tokenService;

	public LoginController(UsuarioService usuarioService, TokenService tokenService) {
		this.usuarioService = usuarioService;
		this.tokenService = tokenService;
	}

	@PostMapping
	public LoginRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
		Usuario usuario = usuarioService.autenticar(peticion.username(), peticion.password());
		return LoginRespuesta.de(tokenService.generar(usuario), usuario);
	}
}
