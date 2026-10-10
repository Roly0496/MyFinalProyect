package com.horneo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.TipoProducto;
import com.horneo.backend.entity.Unidad;
import com.horneo.backend.entity.Usuario;
import com.horneo.backend.entity.Venta;
import com.horneo.backend.exception.CredencialesIncorrectasException;
import com.horneo.backend.exception.ReglaNegocioException;

@SpringBootTest
@Transactional
class CicloPrincipalTest {

	@Autowired
	private MateriaPrimaService materiaPrimaService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private MovimientoService movimientoService;
	@Autowired
	private ProduccionService produccionService;
	@Autowired
	private VentaService ventaService;
	@Autowired
	private UsuarioService usuarioService;

	private Usuario empleado;
	private MateriaPrima harina;
	private MateriaPrima mantequilla;
	private Producto croissant;

	@BeforeEach
	void prepararDatos() {
		empleado = usuarioService.crear("TEST Empleado", "test_empleado", "contraseña123", Rol.EMPLEADO);

		harina = materiaPrimaService.crear("TEST Harina", Unidad.GRAMO, BigDecimal.ZERO, new BigDecimal("0.0008"));
		mantequilla = materiaPrimaService.crear("TEST Mantequilla", Unidad.GRAMO, BigDecimal.ZERO,
				new BigDecimal("0.009"));

		croissant = productoService.crear("TEST Croissant", TipoProducto.ELABORADO, new BigDecimal("1.20"), null);
		productoService.agregarIngrediente(croissant.getId(), harina.getId(), new BigDecimal("50"));
		productoService.agregarIngrediente(croissant.getId(), mantequilla.getId(), new BigDecimal("25"));
	}

	@Test
	void dosEntradasSeguidasSeSuman() {
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("1000"), null, empleado);
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("500"), null, empleado);

		assertThat(materiaPrimaService.obtener(harina.getId()).getStockActual()).isEqualByComparingTo("1500");
	}

	@Test
	void producirRestaMateriasPrimasYSumaProducto() {
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("1000"), null, empleado);
		movimientoService.registrarEntradaMateriaPrima(mantequilla.getId(), new BigDecimal("500"), null, empleado);

		produccionService.registrar(croissant.getId(), 10, empleado);

		assertThat(materiaPrimaService.obtener(harina.getId()).getStockActual()).isEqualByComparingTo("500");
		assertThat(materiaPrimaService.obtener(mantequilla.getId()).getStockActual()).isEqualByComparingTo("250");
		assertThat(productoService.obtener(croissant.getId()).getStockActual()).isEqualTo(10);
	}

	@Test
	void producirSinStockSuficienteFalla() {
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("100"), null, empleado);
		movimientoService.registrarEntradaMateriaPrima(mantequilla.getId(), new BigDecimal("500"), null, empleado);

		assertThatThrownBy(() -> produccionService.registrar(croissant.getId(), 10, empleado))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("No hay stock suficiente de TEST Harina");
	}

	@Test
	void calcularCosteDeLaReceta() {
		// 50 g × 0,0008 €/g + 25 g × 0,009 €/g = 0,04 + 0,225 = 0,265 → 0,27
		assertThat(productoService.calcularCoste(croissant.getId())).isEqualByComparingTo("0.27");
	}

	@Test
	void venderRestaStockYCalculaTotal() {
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("1000"), null, empleado);
		movimientoService.registrarEntradaMateriaPrima(mantequilla.getId(), new BigDecimal("500"), null, empleado);
		produccionService.registrar(croissant.getId(), 10, empleado);

		Producto zumo = productoService.crear("TEST Zumo", TipoProducto.REVENTA, new BigDecimal("2.00"),
				new BigDecimal("0.80"));
		movimientoService.registrarEntradaProducto(zumo.getId(), 5, null, empleado);

		Venta venta = ventaService.registrar(List.of(
				new VentaService.Linea(croissant.getId(), 3),
				new VentaService.Linea(zumo.getId(), 2)), empleado);

		// 3 × 1,20 + 2 × 2,00 = 7,60
		assertThat(venta.getTotal()).isEqualByComparingTo("7.60");
		assertThat(ventaService.obtenerDetalle(venta.getId())).hasSize(2);
		assertThat(productoService.obtener(croissant.getId()).getStockActual()).isEqualTo(7);
		assertThat(productoService.obtener(zumo.getId()).getStockActual()).isEqualTo(3);
	}

	@Test
	void venderSinStockSuficienteFalla() {
		assertThatThrownBy(() -> ventaService.registrar(
				List.of(new VentaService.Linea(croissant.getId(), 1)), empleado))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("No hay stock suficiente de TEST Croissant");
	}

	@Test
	void ajustarStockGuardaLaDiferencia() {
		movimientoService.registrarEntradaMateriaPrima(harina.getId(), new BigDecimal("1000"), null, empleado);

		var ajuste = movimientoService.ajustarStockMateriaPrima(harina.getId(), new BigDecimal("800"),
				"Recuento semanal", empleado);

		assertThat(ajuste.getCantidad()).isEqualByComparingTo("-200");
		assertThat(materiaPrimaService.obtener(harina.getId()).getStockActual()).isEqualByComparingTo("800");
	}

	@Test
	void loginConContraseñaCorrectaEIncorrecta() {
		assertThat(usuarioService.autenticar("test_empleado", "contraseña123").getId()).isEqualTo(empleado.getId());

		assertThatThrownBy(() -> usuarioService.autenticar("test_empleado", "otraCosa99"))
				.isInstanceOf(CredencialesIncorrectasException.class);
		assertThatThrownBy(() -> usuarioService.autenticar("no_existe", "contraseña123"))
				.isInstanceOf(CredencialesIncorrectasException.class);
	}
}
