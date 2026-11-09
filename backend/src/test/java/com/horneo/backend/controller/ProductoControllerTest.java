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

import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.ProductoService;
import com.horneo.backend.service.TokenService;
import com.horneo.backend.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private TokenService tokenService;

	private Usuario admin;
	private Usuario empleado;
	private Producto reventa;

	@BeforeEach
	void prepararDatos() {
		admin = usuarioService.crear("TEST Admin", "test_admin", "contraseña123", Rol.ADMIN);
		empleado = usuarioService.crear("TEST Empleado", "test_empleado", "contraseña123", Rol.EMPLEADO);
		reventa = productoService.crear("TEST Zumo", TipoProducto.REVENTA, new BigDecimal("1.80"),
				new BigDecimal("0.95"));
	}

	@Test
	void unEmpleadoNoRecibeElPrecioDeCompra() throws Exception {
		String token = "Bearer " + tokenService.generar(empleado);

		mockMvc.perform(get("/api/productos/" + reventa.getId()).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precioVenta").value(1.80))
				.andExpect(jsonPath("$.precioCompra").value(nullValue()));

		mockMvc.perform(get("/api/productos").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].precioCompra").value(everyItem(nullValue())));
	}

	@Test
	void unAdministradorSiRecibeElPrecioDeCompra() throws Exception {
		mockMvc.perform(get("/api/productos/" + reventa.getId())
				.header("Authorization", "Bearer " + tokenService.generar(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precioCompra").value(0.95));
	}
}
