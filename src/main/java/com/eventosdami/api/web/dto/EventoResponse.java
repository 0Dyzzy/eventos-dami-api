package com.eventosdami.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.eventosdami.api.domain.Evento;

/**
 * Representación pública de un evento.
 */
public record EventoResponse(
		Long id,
		String nombre,
		LocalDateTime fecha,
		BigDecimal precio,
		int entradasDisponibles) {

	public static EventoResponse desde(Evento evento) {
		return new EventoResponse(
				evento.getId(),
				evento.getNombre(),
				evento.getFecha(),
				evento.getPrecio(),
				evento.getEntradasDisponibles());
	}
}
