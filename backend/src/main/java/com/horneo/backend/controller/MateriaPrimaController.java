package com.horneo.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.config.SoloAdmin;
import com.horneo.backend.dto.ActualizarMateriaPrimaPeticion;
import com.horneo.backend.dto.CrearMateriaPrimaPeticion;
import com.horneo.backend.dto.MateriaPrimaRespuesta;
import com.horneo.backend.dto.MovimientoRespuesta;
import com.horneo.backend.service.MateriaPrimaService;
import com.horneo.backend.service.MovimientoService;

@RestController
@RequestMapping("/api/materias-primas")
public class MateriaPrimaController {

	private final MateriaPrimaService materiaPrimaService;
	private final MovimientoService movimientoService;

	public MateriaPrimaController(MateriaPrimaService materiaPrimaService, MovimientoService movimientoService) {
		this.materiaPrimaService = materiaPrimaService;
		this.movimientoService = movimientoService;
	}

	@GetMapping
	public List<MateriaPrimaRespuesta> listar() {
		return materiaPrimaService.listarActivas().stream()
				.map(MateriaPrimaRespuesta::de)
				.toList();
	}

	@GetMapping("/{id}")
	public MateriaPrimaRespuesta obtener(@PathVariable Long id) {
		return MateriaPrimaRespuesta.de(materiaPrimaService.obtener(id));
	}

	@PostMapping
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public MateriaPrimaRespuesta crear(@Valid @RequestBody CrearMateriaPrimaPeticion peticion) {
		return MateriaPrimaRespuesta.de(materiaPrimaService.crear(peticion.nombre(), peticion.unidad(),
				peticion.stockMinimo(), peticion.precioUnitario()));
	}

	@PutMapping("/{id}")
	@SoloAdmin
	public MateriaPrimaRespuesta actualizar(@PathVariable Long id,
			@Valid @RequestBody ActualizarMateriaPrimaPeticion peticion) {
		return MateriaPrimaRespuesta.de(materiaPrimaService.actualizar(id, peticion.nombre(),
				peticion.stockMinimo(), peticion.precioUnitario()));
	}

	@DeleteMapping("/{id}")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desactivar(@PathVariable Long id) {
		materiaPrimaService.desactivar(id);
	}

	@GetMapping("/{id}/movimientos")
	public List<MovimientoRespuesta> historial(@PathVariable Long id) {
		return movimientoService.historialMateriaPrima(id).stream()
				.map(MovimientoRespuesta::de)
				.toList();
	}
}
