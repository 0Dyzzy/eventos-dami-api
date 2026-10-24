package com.eventosdami.api.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.eventosdami.api.domain.Compra;

/**
 * Resumen de una compra: evento, cantidad, precio unitario y precio total.
 */
public record CompraResponse(
		Long id,
		EventoResponse evento,
		int cantidad,
		BigDecimal precioUnitario,
		BigDecimal precioTotal,
		Instant fechaCompra) {

	public static CompraResponse desde(Compra compra) {
		return new CompraResponse(
				compra.getId(),
				EventoResponse.desde(compra.getEvento()),
				compra.getCantidad(),
				compra.getPrecioUnitario(),
				compra.getPrecioTotal(),
				compra.getFechaCompra());
	}
}
