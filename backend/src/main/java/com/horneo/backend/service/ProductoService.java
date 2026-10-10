package com.horneo.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.Ingrediente;
import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.IngredienteRepository;
import com.horneo.backend.repository.ProductoRepository;

@Service
@Transactional(readOnly = true)
public class ProductoService {

	private final ProductoRepository productoRepository;
	private final IngredienteRepository ingredienteRepository;
	private final MateriaPrimaService materiaPrimaService;

	public ProductoService(ProductoRepository productoRepository, IngredienteRepository ingredienteRepository,
			MateriaPrimaService materiaPrimaService) {
		this.productoRepository = productoRepository;
		this.ingredienteRepository = ingredienteRepository;
		this.materiaPrimaService = materiaPrimaService;
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

	public List<Ingrediente> obtenerReceta(Long productoId) {
		Producto producto = obtener(productoId);
		comprobarElaborado(producto);
		return ingredienteRepository.findByProductoId(productoId);
	}

	@Transactional
	public Ingrediente agregarIngrediente(Long productoId, Long materiaPrimaId, BigDecimal cantidad) {
		Producto producto = obtener(productoId);
		comprobarElaborado(producto);
		MateriaPrima materiaPrima = materiaPrimaService.obtener(materiaPrimaId);
		if (!materiaPrima.isActivo()) {
			throw new ReglaNegocioException("La materia prima " + materiaPrima.getNombre() + " está desactivada");
		}
		if (ingredienteRepository.existsByProductoIdAndMateriaPrimaId(productoId, materiaPrimaId)) {
			throw new ReglaNegocioException(materiaPrima.getNombre() + " ya está en la receta de " + producto.getNombre());
		}
		return ingredienteRepository.save(new Ingrediente(producto, materiaPrima, cantidad));
	}

	@Transactional
	public Ingrediente cambiarCantidadIngrediente(Long productoId, Long ingredienteId, BigDecimal cantidad) {
		Ingrediente ingrediente = obtenerIngrediente(productoId, ingredienteId);
		ingrediente.setCantidad(cantidad);
		return ingrediente;
	}

	@Transactional
	public void quitarIngrediente(Long productoId, Long ingredienteId) {
		Ingrediente ingrediente = obtenerIngrediente(productoId, ingredienteId);
		ingredienteRepository.delete(ingrediente);
	}

	public BigDecimal calcularCoste(Long productoId) {
		Producto producto = obtener(productoId);
		if (producto.getTipo() == TipoProducto.REVENTA) {
			return producto.getPrecioCompra();
		}
		BigDecimal coste = BigDecimal.ZERO;
		for (Ingrediente ingrediente : ingredienteRepository.findByProductoId(productoId)) {
			BigDecimal costeIngrediente = ingrediente.getCantidad()
					.multiply(ingrediente.getMateriaPrima().getPrecioUnitario());
			coste = coste.add(costeIngrediente);
		}
		return coste.setScale(2, RoundingMode.HALF_UP);
	}

	private Ingrediente obtenerIngrediente(Long productoId, Long ingredienteId) {
		Ingrediente ingrediente = ingredienteRepository.findConMateriaPrimaById(ingredienteId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el ingrediente con id " + ingredienteId));
		if (!ingrediente.getProducto().getId().equals(productoId)) {
			throw new RecursoNoEncontradoException(
					"El ingrediente " + ingredienteId + " no pertenece al producto " + productoId);
		}
		return ingrediente;
	}

	private void comprobarElaborado(Producto producto) {
		if (producto.getTipo() != TipoProducto.ELABORADO) {
			throw new ReglaNegocioException(producto.getNombre() + " es de reventa: no tiene receta");
		}
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
