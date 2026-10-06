package com.horneo.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Línea de una venta. El precio se copia del producto en el momento de la venta
 * para que el histórico no cambie si el precio sube o baja.
 */
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venta_id")
	private Venta venta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	private int cantidad;

	private BigDecimal precioUnitario;

	protected DetalleVenta() {
	}

	public DetalleVenta(Venta venta, Producto producto, int cantidad, BigDecimal precioUnitario) {
		this.venta = venta;
		this.producto = producto;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
	}

	public Long getId() {
		return id;
	}

	public Venta getVenta() {
		return venta;
	}

	public Producto getProducto() {
		return producto;
	}

	public int getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}
}
