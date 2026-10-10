package com.horneo.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.horneo.backend.entity.TipoProducto;

public record CrearProductoPeticion(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
		String nombre,

		@NotNull(message = "El tipo es obligatorio")
		TipoProducto tipo,

		@NotNull(message = "El precio de venta es obligatorio")
		@PositiveOrZero(message = "El precio de venta no puede ser negativo")
		BigDecimal precioVenta,

		@PositiveOrZero(message = "El precio de compra no puede ser negativo")
		BigDecimal precioCompra) {
}
