package com.horneo.backend.service;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Movimiento;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoMovimiento;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.MovimientoRepository;

@Service
@Transactional(readOnly = true)
public class MovimientoService {

	private final MovimientoRepository movimientoRepository;
	private final MateriaPrimaService materiaPrimaService;
	private final ProductoService productoService;
	private final EntityManager entityManager;

	public MovimientoService(MovimientoRepository movimientoRepository, MateriaPrimaService materiaPrimaService,
			ProductoService productoService, EntityManager entityManager) {
		this.movimientoRepository = movimientoRepository;
		this.materiaPrimaService = materiaPrimaService;
		this.productoService = productoService;
		this.entityManager = entityManager;
	}

	public List<Movimiento> historialMateriaPrima(Long materiaPrimaId) {
		materiaPrimaService.obtener(materiaPrimaId);
		return movimientoRepository.findByMateriaPrimaIdOrderByFechaDescIdDesc(materiaPrimaId);
	}

	public List<Movimiento> historialProducto(Long productoId) {
		productoService.obtener(productoId);
		return movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId);
	}

	public List<Movimiento> ultimosMovimientos() {
		return movimientoRepository.findTop100ByOrderByFechaDescIdDesc();
	}

	@Transactional
	public Movimiento aplicar(Movimiento movimiento) {
		validarSigno(movimiento);
		if (movimiento.getMateriaPrima() != null) {
			aplicarAMateriaPrima(movimiento.getMateriaPrima(), movimiento.getCantidad());
		} else {
			aplicarAProducto(movimiento.getProducto(), movimiento.getCantidad().intValueExact());
		}
		return movimientoRepository.save(movimiento);
	}

	@Transactional
	public Movimiento registrarEntradaMateriaPrima(Long materiaPrimaId, BigDecimal cantidad, String motivo,
			Usuario usuario) {
		MateriaPrima materiaPrima = materiaPrimaService.obtener(materiaPrimaId);
		return aplicar(Movimiento.deMateriaPrima(TipoMovimiento.ENTRADA, materiaPrima, cantidad, usuario)
				.conMotivo(motivo));
	}

	@Transactional
	public Movimiento registrarEntradaProducto(Long productoId, int cantidad, String motivo, Usuario usuario) {
		Producto producto = productoService.obtener(productoId);
		if (producto.getTipo() != TipoProducto.REVENTA) {
			throw new ReglaNegocioException(producto.getNombre() + " es elaborado: su stock sube con una producción");
		}
		return aplicar(Movimiento.deProducto(TipoMovimiento.ENTRADA, producto, cantidad, usuario)
				.conMotivo(motivo));
	}

	@Transactional
	public Movimiento registrarMermaMateriaPrima(Long materiaPrimaId, BigDecimal cantidad, String motivo,
			Usuario usuario) {
		String motivoLimpio = exigirMotivo(motivo);
		MateriaPrima materiaPrima = materiaPrimaService.obtener(materiaPrimaId);
		return aplicar(Movimiento.deMateriaPrima(TipoMovimiento.MERMA, materiaPrima, cantidad.negate(), usuario)
				.conMotivo(motivoLimpio));
	}

	@Transactional
	public Movimiento registrarMermaProducto(Long productoId, int cantidad, String motivo, Usuario usuario) {
		String motivoLimpio = exigirMotivo(motivo);
		Producto producto = productoService.obtener(productoId);
		return aplicar(Movimiento.deProducto(TipoMovimiento.MERMA, producto, -cantidad, usuario)
				.conMotivo(motivoLimpio));
	}

	@Transactional
	public Movimiento ajustarStockMateriaPrima(Long materiaPrimaId, BigDecimal stockReal, String motivo,
			Usuario usuario) {
		String motivoLimpio = exigirMotivo(motivo);
		MateriaPrima materiaPrima = materiaPrimaService.obtener(materiaPrimaId);
		bloquearYReleer(materiaPrima);
		BigDecimal diferencia = stockReal.subtract(materiaPrima.getStockActual());
		if (diferencia.signum() == 0) {
			throw new ReglaNegocioException("El stock de " + materiaPrima.getNombre() + " ya es " + stockReal);
		}
		return aplicar(Movimiento.deMateriaPrima(TipoMovimiento.AJUSTE, materiaPrima, diferencia, usuario)
				.conMotivo(motivoLimpio));
	}

	@Transactional
	public Movimiento ajustarStockProducto(Long productoId, int stockReal, String motivo, Usuario usuario) {
		String motivoLimpio = exigirMotivo(motivo);
		Producto producto = productoService.obtener(productoId);
		bloquearYReleer(producto);
		int diferencia = stockReal - producto.getStockActual();
		if (diferencia == 0) {
			throw new ReglaNegocioException("El stock de " + producto.getNombre() + " ya es " + stockReal);
		}
		return aplicar(Movimiento.deProducto(TipoMovimiento.AJUSTE, producto, diferencia, usuario)
				.conMotivo(motivoLimpio));
	}

	private void bloquearYReleer(Object entidad) {
		entityManager.flush();
		entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
	}

	private String exigirMotivo(String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new ReglaNegocioException("Hay que indicar el motivo");
		}
		return motivo.trim();
	}

	private void aplicarAMateriaPrima(MateriaPrima materiaPrima, BigDecimal cantidad) {
		bloquearYReleer(materiaPrima);
		BigDecimal stockNuevo = materiaPrima.getStockActual().add(cantidad);
		if (stockNuevo.signum() < 0) {
			throw new ReglaNegocioException("No hay stock suficiente de " + materiaPrima.getNombre()
					+ " (disponible: " + materiaPrima.getStockActual() + ", necesario: " + cantidad.negate() + ")");
		}
		materiaPrima.setStockActual(stockNuevo);
	}

	private void aplicarAProducto(Producto producto, int cantidad) {
		bloquearYReleer(producto);
		int stockNuevo = producto.getStockActual() + cantidad;
		if (stockNuevo < 0) {
			throw new ReglaNegocioException("No hay stock suficiente de " + producto.getNombre()
					+ " (disponible: " + producto.getStockActual() + ", necesario: " + (-cantidad) + ")");
		}
		producto.setStockActual(stockNuevo);
	}

	private void validarSigno(Movimiento movimiento) {
		int signo = movimiento.getCantidad().signum();
		switch (movimiento.getTipo()) {
			case ENTRADA -> {
				if (signo <= 0) {
					throw new ReglaNegocioException("Una entrada debe sumar stock");
				}
			}
			case VENTA, MERMA -> {
				if (signo >= 0) {
					throw new ReglaNegocioException("Una venta o una merma debe restar stock");
				}
			}
			case PRODUCCION, AJUSTE -> {
				if (signo == 0) {
					throw new ReglaNegocioException("Un movimiento no puede tener cantidad 0");
				}
			}
		}
	}
}
