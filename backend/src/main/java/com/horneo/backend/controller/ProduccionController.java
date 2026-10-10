package com.horneo.backend.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.config.AutenticacionInterceptor;
import com.horneo.backend.dto.ProduccionRespuesta;
import com.horneo.backend.dto.RegistrarProduccionPeticion;
import com.horneo.backend.entity.Produccion;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.ProduccionService;

@RestController
@RequestMapping("/api/producciones")
public class ProduccionController {

	private final ProduccionService produccionService;

	public ProduccionController(ProduccionService produccionService) {
		this.produccionService = produccionService;
	}

	@GetMapping
	public List<ProduccionRespuesta> listar(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		List<Produccion> producciones = fecha == null
				? produccionService.listarRecientes()
				: produccionService.listarDelDia(fecha);
		return producciones.stream()
				.map(ProduccionRespuesta::de)
				.toList();
	}

	@GetMapping("/{id}")
	public ProduccionRespuesta obtener(@PathVariable Long id) {
		return ProduccionRespuesta.de(produccionService.obtener(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProduccionRespuesta registrar(@Valid @RequestBody RegistrarProduccionPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return ProduccionRespuesta.de(produccionService.registrar(peticion.productoId(), peticion.cantidad(), usuario));
	}
}
