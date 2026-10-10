package com.horneo.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.Ingrediente;
import com.horneo.backend.entity.Movimiento;
import com.horneo.backend.entity.Produccion;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoMovimiento;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.ProduccionRepository;

@Service
@Transactional(readOnly = true)
public class ProduccionService {

	private final ProduccionRepository produccionRepository;
	private final ProductoService productoService;
	private final MovimientoService movimientoService;

	public ProduccionService(ProduccionRepository produccionRepository, ProductoService productoService,
			MovimientoService movimientoService) {
		this.produccionRepository = produccionRepository;
		this.productoService = productoService;
		this.movimientoService = movimientoService;
	}

	public List<Produccion> listarRecientes() {
		return produccionRepository.findTop100ByOrderByFechaDescIdDesc();
	}

	public List<Produccion> listarDelDia(LocalDate dia) {
		return produccionRepository.findByFechaBetweenOrderByFechaDescIdDesc(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
	}

	public Produccion obtener(Long id) {
		return produccionRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la producción con id " + id));
	}

	@Transactional
	public Produccion registrar(Long productoId, int cantidad, Usuario usuario) {
		if (cantidad <= 0) {
			throw new ReglaNegocioException("La cantidad a producir debe ser mayor que 0");
		}
		Producto producto = productoService.obtener(productoId);
		if (!producto.isActivo()) {
			throw new ReglaNegocioException(producto.getNombre() + " está desactivado");
		}
		List<Ingrediente> receta = productoService.obtenerReceta(productoId);
		if (receta.isEmpty()) {
			throw new ReglaNegocioException(producto.getNombre() + " no tiene receta");
		}

		Produccion produccion = produccionRepository.save(new Produccion(producto, cantidad, usuario));

		List<Ingrediente> recetaOrdenada = receta.stream()
				.sorted(Comparator.comparing(ingrediente -> ingrediente.getMateriaPrima().getId()))
				.toList();
		for (Ingrediente ingrediente : recetaOrdenada) {
			BigDecimal gasto = ingrediente.getCantidad().multiply(BigDecimal.valueOf(cantidad)).negate();
			movimientoService.aplicar(Movimiento.deMateriaPrima(TipoMovimiento.PRODUCCION,
					ingrediente.getMateriaPrima(), gasto, usuario).conProduccion(produccion));
		}
		movimientoService.aplicar(Movimiento.deProducto(TipoMovimiento.PRODUCCION, producto, cantidad, usuario)
				.conProduccion(produccion));

		return produccion;
	}
}
