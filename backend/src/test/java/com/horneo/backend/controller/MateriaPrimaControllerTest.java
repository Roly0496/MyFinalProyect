package com.horneo.backend.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Unidad;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.MateriaPrimaService;
import com.horneo.backend.service.TokenService;
import com.horneo.backend.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MateriaPrimaControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private MateriaPrimaService materiaPrimaService;
	@Autowired
	private TokenService tokenService;

	private Usuario admin;
	private Usuario empleado;
	private MateriaPrima azucar;

	@BeforeEach
	void prepararDatos() {
		admin = usuarioService.crear("TEST Admin", "test_admin", "contraseña123", Rol.ADMIN);
		empleado = usuarioService.crear("TEST Empleado", "test_empleado", "contraseña123", Rol.EMPLEADO);
		azucar = materiaPrimaService.crear("TEST Azúcar", Unidad.GRAMO, new BigDecimal("1000"),
				new BigDecimal("0.0012"));
	}

	@Test
	void unEmpleadoNoRecibeElPrecioUnitario() throws Exception {
		String token = "Bearer " + tokenService.generar(empleado);

		// El resto de datos sí llegan: el empleado necesita el stock para trabajar.
		mockMvc.perform(get("/api/materias-primas/" + azucar.getId()).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("TEST Azúcar"))
				.andExpect(jsonPath("$.bajoMinimo").value(true))
				.andExpect(jsonPath("$.precioUnitario").value(nullValue()));

		mockMvc.perform(get("/api/materias-primas").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].precioUnitario").value(everyItem(nullValue())));
	}

	@Test
	void unAdministradorSiRecibeElPrecioUnitario() throws Exception {
		mockMvc.perform(get("/api/materias-primas/" + azucar.getId())
				.header("Authorization", "Bearer " + tokenService.generar(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precioUnitario").value(0.0012));
	}
}
