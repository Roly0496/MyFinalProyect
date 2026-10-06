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
import java.time.LocalDateTime;

/**
 * Cabecera de una venta (el "ticket"). Las líneas están en DetalleVenta.
 */
@Entity
@Table(name = "venta")
public class Venta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime fecha = LocalDateTime.now();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	private BigDecimal total;

	protected Venta() {
	}

	public Venta(Usuario usuario, BigDecimal total) {
		this.usuario = usuario;
		this.total = total;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public BigDecimal getTotal() {
		return total;
	}
}
