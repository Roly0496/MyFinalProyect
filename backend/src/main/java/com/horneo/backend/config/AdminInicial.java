package com.horneo.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.repository.UsuarioRepository;
import com.horneo.backend.service.UsuarioService;

@Component
public class AdminInicial implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;
	private final String username;
	private final String password;

	public AdminInicial(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			@Value("${horneo.admin.username:admin}") String username,
			@Value("${horneo.admin.password:}") String password) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.username = username;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) > 0) {
			return;
		}
		if (password.isBlank()) {
			log.warn("No hay ningún administrador activo y no se ha definido horneo.admin.password: "
					+ "no se podrá iniciar sesión como administrador");
			return;
		}
		usuarioService.crear("Administrador", username, password, Rol.ADMIN);
		log.info("Creado el administrador inicial '{}'", username);
	}
}
