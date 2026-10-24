package com.eventosdami.api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento disponible para la venta de entradas.
 */
@Entity
@Table(name = "eventos")
public class Evento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal precio;

	@Column(name = "entradas_disponibles", nullable = false)
	private int entradasDisponibles;

	/** Requerido por JPA. */
	protected Evento() {
	}

	public Evento(String nombre, LocalDateTime fecha, BigDecimal precio, int entradasDisponibles) {
		this.nombre = nombre;
		this.fecha = fecha;
		this.precio = precio.setScale(2, RoundingMode.HALF_UP);
		this.entradasDisponibles = entradasDisponibles;
	}

	/**
	 * Descuenta entradas del stock del evento.
	 *
	 * @throws IllegalArgumentException si la cantidad no es positiva
	 * @throws IllegalStateException si no hay stock suficiente
	 */
	public void descontarEntradas(int cantidad) {
		if (cantidad < 1) {
			throw new IllegalArgumentException("La cantidad de entradas debe ser mayor a cero");
		}
		if (cantidad > this.entradasDisponibles) {
			throw new IllegalStateException("No hay entradas suficientes para el evento " + this.nombre);
		}
		this.entradasDisponibles -= cantidad;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public int getEntradasDisponibles() {
		return entradasDisponibles;
	}

}
