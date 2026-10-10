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
import com.horneo.backend.config.SoloAdmin;
import com.horneo.backend.dto.CajaRespuesta;
import com.horneo.backend.dto.LineaVentaRespuesta;
import com.horneo.backend.dto.RegistrarVentaPeticion;
import com.horneo.backend.dto.TicketRespuesta;
import com.horneo.backend.dto.VentaRespuesta;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.entity.Venta;
import com.horneo.backend.service.VentaService;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

	private final VentaService ventaService;

	public VentaController(VentaService ventaService) {
		this.ventaService = ventaService;
	}

	@GetMapping
	public List<VentaRespuesta> listar(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		List<Venta> ventas = fecha == null
				? ventaService.listarRecientes()
				: ventaService.listarDelDia(fecha);
		return ventas.stream()
				.map(VentaRespuesta::de)
				.toList();
	}

	@GetMapping("/{id}")
	public TicketRespuesta obtener(@PathVariable Long id) {
		return ticket(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TicketRespuesta registrar(@Valid @RequestBody RegistrarVentaPeticion peticion,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		List<VentaService.Linea> lineas = peticion.lineas().stream()
				.map(linea -> new VentaService.Linea(linea.productoId(), linea.cantidad()))
				.toList();
		Venta venta = ventaService.registrar(lineas, usuario);
		return ticket(venta.getId());
	}

	@GetMapping("/caja")
	@SoloAdmin
	public CajaRespuesta caja(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		LocalDate dia = fecha == null ? LocalDate.now() : fecha;
		return new CajaRespuesta(dia, ventaService.listarDelDia(dia).size(), ventaService.totalDelDia(dia));
	}

	private TicketRespuesta ticket(Long ventaId) {
		VentaRespuesta venta = VentaRespuesta.de(ventaService.obtener(ventaId));
		List<LineaVentaRespuesta> lineas = ventaService.obtenerDetalle(ventaId).stream()
				.map(LineaVentaRespuesta::de)
				.toList();
		return new TicketRespuesta(venta, lineas);
	}
}
