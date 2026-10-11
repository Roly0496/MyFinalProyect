package com.horneo.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.TokenService;
import com.horneo.backend.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private TokenService tokenService;

	private Usuario admin;
	private Usuario empleado;

	@BeforeEach
	void prepararUsuarios() {
		admin = usuarioService.crear("TEST Admin", "test_admin", "contraseña123", Rol.ADMIN);
		empleado = usuarioService.crear("TEST Empleado", "test_empleado", "contraseña123", Rol.EMPLEADO);
	}

	@Test
	void unEmpleadoNoPuedeGestionarUsuarios() throws Exception {
		String json = """
				{"nombre": "Otro", "username": "test_otro", "password": "contraseña123", "rol": "ADMIN"}
				""";
		MockHttpServletRequestBuilder[] peticionesDeAdmin = {
				get("/api/usuarios"),
				get("/api/usuarios/" + admin.getId()),
				post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(json),
				put("/api/usuarios/" + empleado.getId()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\": \"Ascendido\", \"rol\": \"ADMIN\"}"),
				put("/api/usuarios/" + admin.getId() + "/password").contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\": \"hackeada123\"}"),
				delete("/api/usuarios/" + admin.getId()),
				post("/api/usuarios/" + admin.getId() + "/activar")
		};
		for (MockHttpServletRequestBuilder peticion : peticionesDeAdmin) {
			mockMvc.perform(peticion.header("Authorization", "Bearer " + tokenService.generar(empleado)))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void unAdministradorSiPuedeVerLosUsuarios() throws Exception {
		mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenService.generar(admin)))
				.andExpect(status().isOk());
	}

	@Test
	void sinTokenNoSePuedeEntrar() throws Exception {
		mockMvc.perform(get("/api/usuarios/yo"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cualquierUsuarioVeSusDatosSinElHashDeLaContraseña() throws Exception {
		mockMvc.perform(get("/api/usuarios/yo").header("Authorization", "Bearer " + tokenService.generar(empleado)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("test_empleado"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void cambiarMiContraseñaExigeLaContraseñaActual() throws Exception {
		String token = "Bearer " + tokenService.generar(empleado);

		mockMvc.perform(put("/api/usuarios/yo/password").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"passwordActual\": \"noEsLaMia1\", \"passwordNueva\": \"nuevaClave456\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(put("/api/usuarios/yo/password").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"passwordActual\": \"contraseña123\", \"passwordNueva\": \"nuevaClave456\"}"))
				.andExpect(status().isNoContent());

		usuarioService.autenticar("test_empleado", "nuevaClave456");
	}
}
