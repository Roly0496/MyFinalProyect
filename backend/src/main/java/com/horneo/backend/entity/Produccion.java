package com.horneo.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro histórico de una producción. Una vez creada no se modifica,
 * por eso no tiene setters.
 */
@Entity
@Table(name = "produccion")
public class Produccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime fecha = LocalDateTime.now();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	private int cantidad;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	protected Produccion() {
	}

	public Produccion(Producto producto, int cantidad, Usuario usuario) {
		this.producto = producto;
		this.cantidad = cantidad;
		this.usuario = usuario;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public Producto getProducto() {
		return producto;
	}

	public int getCantidad() {
		return cantidad;
	}

	public Usuario getUsuario() {
		return usuario;
	}
}
