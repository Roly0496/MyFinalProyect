package com.horneo.backend.dto;

import java.util.List;

public record TicketRespuesta(VentaRespuesta venta, List<LineaVentaRespuesta> lineas) {
}
