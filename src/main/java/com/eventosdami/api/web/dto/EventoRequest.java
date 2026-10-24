package com.eventosdami.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un evento.
 */
public record EventoRequest(

		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
		String nombre,

		@NotNull(message = "La fecha es obligatoria")
		LocalDateTime fecha,

		@NotNull(message = "El precio es obligatorio")
		@DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
		@Digits(integer = 10, fraction = 2, message = "El precio admite como máximo 2 decimales")
		BigDecimal precio,

		@NotNull(message = "Las entradas disponibles son obligatorias")
		@Min(value = 0, message = "Las entradas disponibles no pueden ser negativas")
		Integer entradasDisponibles) {
}
