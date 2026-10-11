package com.horneo.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.config.AutenticacionInterceptor;
import com.horneo.backend.config.SoloAdmin;
import com.horneo.backend.dto.ActualizarUsuarioPeticion;
import com.horneo.backend.dto.CambiarMiPasswordPeticion;
import com.horneo.backend.dto.CrearUsuarioPeticion;
import com.horneo.backend.dto.NuevaPasswordPeticion;
import com.horneo.backend.dto.UsuarioRespuesta;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/yo")
	public UsuarioRespuesta yo(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return UsuarioRespuesta.de(usuario);
	}

	@PutMapping("/yo/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cambiarMiPassword(@Valid @RequestBody CambiarMiPasswordPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		usuarioService.cambiarPasswordPropia(usuario.getId(), peticion.passwordActual(), peticion.passwordNueva());
	}

	@GetMapping
	@SoloAdmin
	public List<UsuarioRespuesta> listar() {
		return usuarioService.listarTodos().stream()
				.map(UsuarioRespuesta::de)
				.toList();
	}

	@GetMapping("/{id}")
	@SoloAdmin
	public UsuarioRespuesta obtener(@PathVariable Long id) {
		return UsuarioRespuesta.de(usuarioService.obtener(id));
	}

	@PostMapping
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioRespuesta crear(@Valid @RequestBody CrearUsuarioPeticion peticion) {
		return UsuarioRespuesta.de(usuarioService.crear(peticion.nombre(), peticion.username(), peticion.password(),
				peticion.rol()));
	}

	@PutMapping("/{id}")
	@SoloAdmin
	public UsuarioRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioPeticion peticion) {
		return UsuarioRespuesta.de(usuarioService.actualizar(id, peticion.nombre(), peticion.rol()));
	}

	@PutMapping("/{id}/password")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cambiarPassword(@PathVariable Long id, @Valid @RequestBody NuevaPasswordPeticion peticion) {
		usuarioService.cambiarPassword(id, peticion.password());
	}

	@DeleteMapping("/{id}")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desactivar(@PathVariable Long id) {
		usuarioService.desactivar(id);
	}

	@PostMapping("/{id}/activar")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activar(@PathVariable Long id) {
		usuarioService.activar(id);
	}
}
