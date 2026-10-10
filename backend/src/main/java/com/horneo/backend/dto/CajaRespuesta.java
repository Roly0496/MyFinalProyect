package com.horneo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CajaRespuesta(LocalDate fecha, int numeroVentas, BigDecimal total) {
}
