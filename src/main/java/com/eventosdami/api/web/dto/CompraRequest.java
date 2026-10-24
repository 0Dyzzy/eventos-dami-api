package com.eventosdami.api.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos para comprar entradas de un evento.
 */
public record CompraRequest(

		@NotNull(message = "El id del evento es obligatorio")
		@Positive(message = "El id del evento debe ser positivo")
		Long eventoId,

		@NotNull(message = "La cantidad de entradas es obligatoria")
		@Positive(message = "La cantidad de entradas debe ser mayor a cero")
		Integer cantidad) {
}
