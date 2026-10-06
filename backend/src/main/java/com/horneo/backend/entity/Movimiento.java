package com.horneo.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de stock. Afecta a una materia prima o a un producto, nunca a los dos.
 * La cantidad lleva signo: positiva si el stock sube y negativa si baja.
 *
 * Se crea con los métodos deMateriaPrima() o deProducto(), que garantizan
 * que solo uno de los dos queda informado.
 */
@Entity
@Table(name = "movimiento")
public class Movimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime fecha = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	private TipoMovimiento tipo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "materia_prima_id")
	private MateriaPrima materiaPrima;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	private BigDecimal cantidad;

	private String motivo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produccion_id")
	private Produccion produccion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venta_id")
	private Venta venta;

	protected Movimiento() {
	}

	private Movimiento(TipoMovimiento tipo, BigDecimal cantidad, Usuario usuario) {
		this.tipo = tipo;
		this.cantidad = cantidad;
		this.usuario = usuario;
	}

	public static Movimiento deMateriaPrima(TipoMovimiento tipo, MateriaPrima materiaPrima,
											BigDecimal cantidad, Usuario usuario) {
		Movimiento movimiento = new Movimiento(tipo, cantidad, usuario);
		movimiento.materiaPrima = materiaPrima;
		return movimiento;
	}

	public static Movimiento deProducto(TipoMovimiento tipo, Producto producto,
										int cantidad, Usuario usuario) {
		Movimiento movimiento = new Movimiento(tipo, BigDecimal.valueOf(cantidad), usuario);
		movimiento.producto = producto;
		return movimiento;
	}

	public Movimiento conMotivo(String motivo) {
		this.motivo = motivo;
		return this;
	}

	public Movimiento conProduccion(Produccion produccion) {
		this.produccion = produccion;
		return this;
	}

	public Movimiento conVenta(Venta venta) {
		this.venta = venta;
		return this;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public MateriaPrima getMateriaPrima() {
		return materiaPrima;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public String getMotivo() {
		return motivo;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Produccion getProduccion() {
		return produccion;
	}

	public Venta getVenta() {
		return venta;
	}
}
