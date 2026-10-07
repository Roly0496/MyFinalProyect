package com.horneo.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.ProductoRepository;

@Service
@Transactional(readOnly = true)
public class ProductoService {

	private final ProductoRepository productoRepository;

	public ProductoService(ProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	public List<Producto> listarActivos() {
		return productoRepository.findByActivoTrueOrderByNombreAsc();
	}

	public List<Producto> listarActivosPorTipo(TipoProducto tipo) {
		return productoRepository.findByTipoAndActivoTrueOrderByNombreAsc(tipo);
	}

	public Producto obtener(Long id) {
		return productoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
	}

	@Transactional
	public Producto crear(String nombre, TipoProducto tipo, BigDecimal precioVenta, BigDecimal precioCompra) {
		String nombreLimpio = nombre.trim();
		if (productoRepository.existsByNombre(nombreLimpio)) {
			throw new ReglaNegocioException("Ya existe un producto llamado " + nombreLimpio);
		}
		validarPrecioCompra(tipo, precioCompra);
		Producto producto = new Producto(nombreLimpio, tipo, precioVenta);
		producto.setPrecioCompra(precioCompra);
		return productoRepository.save(producto);
	}

	@Transactional
	public Producto actualizar(Long id, String nombre, BigDecimal precioVenta, BigDecimal precioCompra) {
		Producto producto = obtener(id);
		String nombreLimpio = nombre.trim();
		boolean nombreUsadoPorOtro = productoRepository.findByNombre(nombreLimpio)
				.filter(otro -> !otro.getId().equals(id))
				.isPresent();
		if (nombreUsadoPorOtro) {
			throw new ReglaNegocioException("Ya existe un producto llamado " + nombreLimpio);
		}
		validarPrecioCompra(producto.getTipo(), precioCompra);
		producto.setNombre(nombreLimpio);
		producto.setPrecioVenta(precioVenta);
		producto.setPrecioCompra(precioCompra);
		return producto;
	}

	@Transactional
	public void desactivar(Long id) {
		Producto producto = obtener(id);
		producto.setActivo(false);
	}

	private void validarPrecioCompra(TipoProducto tipo, BigDecimal precioCompra) {
		if (tipo == TipoProducto.REVENTA && precioCompra == null) {
			throw new ReglaNegocioException("Un producto de reventa necesita precio de compra");
		}
		if (tipo == TipoProducto.ELABORADO && precioCompra != null) {
			throw new ReglaNegocioException("Un producto elaborado no tiene precio de compra: su coste sale de la receta");
		}
	}
}
