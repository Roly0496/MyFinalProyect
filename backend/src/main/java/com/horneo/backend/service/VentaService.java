package com.horneo.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.DetalleVenta;
import com.horneo.backend.entity.Movimiento;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoMovimiento;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.entity.Venta;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.DetalleVentaRepository;
import com.horneo.backend.repository.VentaRepository;

@Service
@Transactional(readOnly = true)
public class VentaService {

	public record Linea(Long productoId, int cantidad) {
	}

	private final VentaRepository ventaRepository;
	private final DetalleVentaRepository detalleVentaRepository;
	private final ProductoService productoService;
	private final MovimientoService movimientoService;

	public VentaService(VentaRepository ventaRepository, DetalleVentaRepository detalleVentaRepository,
			ProductoService productoService, MovimientoService movimientoService) {
		this.ventaRepository = ventaRepository;
		this.detalleVentaRepository = detalleVentaRepository;
		this.productoService = productoService;
		this.movimientoService = movimientoService;
	}

	public List<Venta> listarRecientes() {
		return ventaRepository.findTop100ByOrderByFechaDescIdDesc();
	}

	public List<Venta> listarDelDia(LocalDate dia) {
		return ventaRepository.findByFechaBetweenOrderByFechaDescIdDesc(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
	}

	public BigDecimal totalDelDia(LocalDate dia) {
		BigDecimal total = BigDecimal.ZERO;
		for (Venta venta : listarDelDia(dia)) {
			total = total.add(venta.getTotal());
		}
		return total;
	}

	public Venta obtener(Long id) {
		return ventaRepository.findConUsuarioById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
	}

	public List<DetalleVenta> obtenerDetalle(Long ventaId) {
		obtener(ventaId);
		return detalleVentaRepository.findByVentaIdOrderByIdAsc(ventaId);
	}

	@Transactional
	public Venta registrar(List<Linea> lineas, Usuario usuario) {
		validarLineas(lineas);
		List<Linea> lineasOrdenadas = lineas.stream()
				.sorted(Comparator.comparing(Linea::productoId))
				.toList();

		List<Producto> productos = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (Linea linea : lineasOrdenadas) {
			Producto producto = productoService.obtener(linea.productoId());
			if (!producto.isActivo()) {
				throw new ReglaNegocioException(producto.getNombre() + " está desactivado");
			}
			productos.add(producto);
			total = total.add(producto.getPrecioVenta().multiply(BigDecimal.valueOf(linea.cantidad())));
		}

		Venta venta = ventaRepository.save(new Venta(usuario, total));

		for (int i = 0; i < lineasOrdenadas.size(); i++) {
			Producto producto = productos.get(i);
			int cantidad = lineasOrdenadas.get(i).cantidad();
			detalleVentaRepository.save(new DetalleVenta(venta, producto, cantidad, producto.getPrecioVenta()));
			movimientoService.aplicar(Movimiento.deProducto(TipoMovimiento.VENTA, producto, -cantidad, usuario)
					.conVenta(venta));
		}

		return venta;
	}

	private void validarLineas(List<Linea> lineas) {
		if (lineas == null || lineas.isEmpty()) {
			throw new ReglaNegocioException("Una venta debe tener al menos un producto");
		}
		Set<Long> productosVistos = new HashSet<>();
		for (Linea linea : lineas) {
			if (linea.productoId() == null) {
				throw new ReglaNegocioException("Cada línea de la venta debe indicar un producto");
			}
			if (linea.cantidad() <= 0) {
				throw new ReglaNegocioException("La cantidad de cada producto debe ser mayor que 0");
			}
			if (!productosVistos.add(linea.productoId())) {
				throw new ReglaNegocioException("Un producto no puede aparecer dos veces en la misma venta");
			}
		}
	}
}
