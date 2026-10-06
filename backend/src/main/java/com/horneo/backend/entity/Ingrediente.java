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
 * Línea de la receta de un producto: cantidad de una materia prima
 * necesaria para fabricar 1 unidad de producto.
 */
@Entity
@Table(name = "ingrediente")
public class Ingrediente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "materia_prima_id")
	private MateriaPrima materiaPrima;

	private BigDecimal cantidad;

	protected Ingrediente() {
	}

	public Ingrediente(Producto producto, MateriaPrima materiaPrima, BigDecimal cantidad) {
		this.producto = producto;
		this.materiaPrima = materiaPrima;
		this.cantidad = cantidad;
	}

	public Long getId() {
		return id;
	}

	public Producto getProducto() {
		return producto;
	}

	public MateriaPrima getMateriaPrima() {
		return materiaPrima;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}
}
