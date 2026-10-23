package com.eventosdami.api.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Registro histórico de una compra de entradas.
 * Se guardan los precios al momento de la compra para que un cambio
 * posterior en el evento no altere el comprobante.
 */
@Entity
@Table(name = "compras")
public class Compra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@Column(nullable = false)
	private int cantidad;

	@Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioTotal;

	@Column(name = "fecha_compra", nullable = false)
	private Instant fechaCompra;

	/** Requerido por JPA. */
	protected Compra() {
	}

	public Compra(Evento evento, int cantidad, BigDecimal precioUnitario, BigDecimal precioTotal, Instant fechaCompra) {
		this.evento = evento;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.precioTotal = precioTotal;
		this.fechaCompra = fechaCompra;
	}

	public Long getId() {
		return id;
	}

	public Evento getEvento() {
		return evento;
	}

	public int getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public BigDecimal getPrecioTotal() {
		return precioTotal;
	}

	public Instant getFechaCompra() {
		return fechaCompra;
	}

}
