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

	private void aplicarAMateriaPrima(MateriaPrima materiaPrima, BigDecimal cantidad) {
		entityManager.refresh(materiaPrima, LockModeType.PESSIMISTIC_WRITE);
		BigDecimal stockNuevo = materiaPrima.getStockActual().add(cantidad);
		if (stockNuevo.signum() < 0) {
			throw new ReglaNegocioException("No hay stock suficiente de " + materiaPrima.getNombre()
					+ " (disponible: " + materiaPrima.getStockActual() + ", necesario: " + cantidad.negate() + ")");
		}
		materiaPrima.setStockActual(stockNuevo);
	}

	private void aplicarAProducto(Producto producto, int cantidad) {
		entityManager.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
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
