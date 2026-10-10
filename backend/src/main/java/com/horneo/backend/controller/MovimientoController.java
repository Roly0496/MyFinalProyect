package com.horneo.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.config.AutenticacionInterceptor;
import com.horneo.backend.config.SoloAdmin;
import com.horneo.backend.dto.AjusteMateriaPrimaPeticion;
import com.horneo.backend.dto.AjusteProductoPeticion;
import com.horneo.backend.dto.MovimientoMateriaPrimaPeticion;
import com.horneo.backend.dto.MovimientoProductoPeticion;
import com.horneo.backend.dto.MovimientoRespuesta;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.MovimientoService;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

	private final MovimientoService movimientoService;

	public MovimientoController(MovimientoService movimientoService) {
		this.movimientoService = movimientoService;
	}

	@GetMapping
	public List<MovimientoRespuesta> ultimos() {
		return movimientoService.ultimosMovimientos().stream()
				.map(MovimientoRespuesta::de)
				.toList();
	}

	@PostMapping("/entradas/materias-primas")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta entradaMateriaPrima(@Valid @RequestBody MovimientoMateriaPrimaPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.registrarEntradaMateriaPrima(peticion.materiaPrimaId(),
				peticion.cantidad(), peticion.motivo(), usuario));
	}

	@PostMapping("/entradas/productos")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta entradaProducto(@Valid @RequestBody MovimientoProductoPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.registrarEntradaProducto(peticion.productoId(),
				peticion.cantidad(), peticion.motivo(), usuario));
	}

	@PostMapping("/mermas/materias-primas")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta mermaMateriaPrima(@Valid @RequestBody MovimientoMateriaPrimaPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.registrarMermaMateriaPrima(peticion.materiaPrimaId(),
				peticion.cantidad(), peticion.motivo(), usuario));
	}

	@PostMapping("/mermas/productos")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta mermaProducto(@Valid @RequestBody MovimientoProductoPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.registrarMermaProducto(peticion.productoId(),
				peticion.cantidad(), peticion.motivo(), usuario));
	}

	@PostMapping("/ajustes/materias-primas")
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta ajusteMateriaPrima(@Valid @RequestBody AjusteMateriaPrimaPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.ajustarStockMateriaPrima(peticion.materiaPrimaId(),
				peticion.stockReal(), peticion.motivo(), usuario));
	}

	@PostMapping("/ajustes/productos")
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRespuesta ajusteProducto(@Valid @RequestBody AjusteProductoPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return MovimientoRespuesta.de(movimientoService.ajustarStockProducto(peticion.productoId(),
				peticion.stockReal(), peticion.motivo(), usuario));
	}
}
