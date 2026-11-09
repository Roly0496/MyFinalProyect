package com.horneo.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horneo.backend.config.AutenticacionInterceptor;
import com.horneo.backend.config.SoloAdmin;
import com.horneo.backend.dto.ActualizarProductoPeticion;
import com.horneo.backend.dto.AgregarIngredientePeticion;
import com.horneo.backend.dto.CambiarCantidadPeticion;
import com.horneo.backend.dto.CosteRespuesta;
import com.horneo.backend.dto.CrearProductoPeticion;
import com.horneo.backend.dto.IngredienteRespuesta;
import com.horneo.backend.dto.MovimientoRespuesta;
import com.horneo.backend.dto.ProductoRespuesta;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.service.MovimientoService;
import com.horneo.backend.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private final ProductoService productoService;
	private final MovimientoService movimientoService;

	public ProductoController(ProductoService productoService, MovimientoService movimientoService) {
		this.productoService = productoService;
		this.movimientoService = movimientoService;
	}

	@GetMapping
	public List<ProductoRespuesta> listar(@RequestParam(required = false) TipoProducto tipo,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		List<Producto> productos = tipo == null
				? productoService.listarActivos()
				: productoService.listarActivosPorTipo(tipo);
		return productos.stream()
				.map(producto -> responder(producto, usuario))
				.toList();
	}

	@GetMapping("/{id}")
	public ProductoRespuesta obtener(@PathVariable Long id,
			@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
		return responder(productoService.obtener(id), usuario);
	}

	// Los endpoints que ven los dos roles pasan por aquí: solo el administrador recibe el precio de compra.
	// Crear y actualizar no lo necesitan porque son @SoloAdmin.
	private ProductoRespuesta responder(Producto producto, Usuario usuario) {
		return usuario.getRol() == Rol.ADMIN
				? ProductoRespuesta.de(producto)
				: ProductoRespuesta.sinCostes(producto);
	}

	@PostMapping
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public ProductoRespuesta crear(@Valid @RequestBody CrearProductoPeticion peticion) {
		return ProductoRespuesta.de(productoService.crear(peticion.nombre(), peticion.tipo(),
				peticion.precioVenta(), peticion.precioCompra()));
	}

	@PutMapping("/{id}")
	@SoloAdmin
	public ProductoRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProductoPeticion peticion) {
		return ProductoRespuesta.de(productoService.actualizar(id, peticion.nombre(),
				peticion.precioVenta(), peticion.precioCompra()));
	}

	@DeleteMapping("/{id}")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desactivar(@PathVariable Long id) {
		productoService.desactivar(id);
	}

	@GetMapping("/{id}/receta")
	public List<IngredienteRespuesta> obtenerReceta(@PathVariable Long id) {
		return productoService.obtenerReceta(id).stream()
				.map(IngredienteRespuesta::de)
				.toList();
	}

	@PostMapping("/{id}/receta")
	@SoloAdmin
	@ResponseStatus(HttpStatus.CREATED)
	public IngredienteRespuesta agregarIngrediente(@PathVariable Long id,
			@Valid @RequestBody AgregarIngredientePeticion peticion) {
		return IngredienteRespuesta.de(productoService.agregarIngrediente(id, peticion.materiaPrimaId(),
				peticion.cantidad()));
	}

	@PutMapping("/{id}/receta/{ingredienteId}")
	@SoloAdmin
	public IngredienteRespuesta cambiarCantidadIngrediente(@PathVariable Long id, @PathVariable Long ingredienteId,
			@Valid @RequestBody CambiarCantidadPeticion peticion) {
		return IngredienteRespuesta.de(productoService.cambiarCantidadIngrediente(id, ingredienteId,
				peticion.cantidad()));
	}

	@DeleteMapping("/{id}/receta/{ingredienteId}")
	@SoloAdmin
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitarIngrediente(@PathVariable Long id, @PathVariable Long ingredienteId) {
		productoService.quitarIngrediente(id, ingredienteId);
	}

	@GetMapping("/{id}/coste")
	@SoloAdmin
	public CosteRespuesta calcularCoste(@PathVariable Long id) {
		return new CosteRespuesta(id, productoService.calcularCoste(id));
	}

	@GetMapping("/{id}/movimientos")
	public List<MovimientoRespuesta> historial(@PathVariable Long id) {
		return movimientoService.historialProducto(id).stream()
				.map(MovimientoRespuesta::de)
				.toList();
	}
}
