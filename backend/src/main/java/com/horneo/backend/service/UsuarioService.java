package com.horneo.backend.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.exception.CredencialesIncorrectasException;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private static final int LONGITUD_MINIMA_PASSWORD = 8;

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<Usuario> listarTodos() {
		return usuarioRepository.findAllByOrderByNombreAsc();
	}

	public Usuario obtener(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
	}

	public Usuario autenticar(String username, String password) {
		Usuario usuario = usuarioRepository.findByUsername(username.trim())
				.orElseThrow(CredencialesIncorrectasException::new);
		if (!usuario.isActivo() || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
			throw new CredencialesIncorrectasException();
		}
		return usuario;
	}

	@Transactional
	public Usuario crear(String nombre, String username, String password, Rol rol) {
		String usernameLimpio = username.trim();
		if (usuarioRepository.existsByUsername(usernameLimpio)) {
			throw new ReglaNegocioException("Ya existe el usuario " + usernameLimpio);
		}
		validarPassword(password);
		Usuario usuario = new Usuario(nombre.trim(), usernameLimpio, passwordEncoder.encode(password), rol);
		return usuarioRepository.save(usuario);
	}

	@Transactional
	public Usuario actualizar(Long id, String nombre, Rol rol) {
		Usuario usuario = obtener(id);
		if (rol != Rol.ADMIN) {
			comprobarQueNoEsUltimoAdmin(usuario);
		}
		usuario.setNombre(nombre.trim());
		usuario.setRol(rol);
		return usuario;
	}

	@Transactional
	public void cambiarPassword(Long id, String nuevaPassword) {
		Usuario usuario = obtener(id);
		validarPassword(nuevaPassword);
		usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
	}

	@Transactional
	public void cambiarPasswordPropia(Long id, String passwordActual, String nuevaPassword) {
		Usuario usuario = obtener(id);
		if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
			throw new ReglaNegocioException("La contraseña actual no es correcta");
		}
		validarPassword(nuevaPassword);
		usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
	}

	@Transactional
	public void desactivar(Long id) {
		Usuario usuario = obtener(id);
		comprobarQueNoEsUltimoAdmin(usuario);
		usuario.setActivo(false);
	}

	@Transactional
	public void activar(Long id) {
		Usuario usuario = obtener(id);
		usuario.setActivo(true);
	}

	private void comprobarQueNoEsUltimoAdmin(Usuario usuario) {
		boolean esAdminActivo = usuario.getRol() == Rol.ADMIN && usuario.isActivo();
		if (esAdminActivo && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) == 1) {
			throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
		}
	}

	private void validarPassword(String password) {
		if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
			throw new ReglaNegocioException(
					"La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
		}
	}
}
