package com.eventosdami.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventosdami.api.domain.Compra;
import com.eventosdami.api.domain.Evento;
import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.error.StockInsuficienteException;
import com.eventosdami.api.repository.CompraRepository;
import com.eventosdami.api.repository.EventoRepository;
import com.eventosdami.api.web.dto.CompraRequest;
import com.eventosdami.api.web.dto.CompraResponse;

@Service
public class CompraService {

	private final EventoRepository eventoRepository;

	private final CompraRepository compraRepository;

	public CompraService(EventoRepository eventoRepository, CompraRepository compraRepository) {
		this.eventoRepository = eventoRepository;
		this.compraRepository = compraRepository;
	}

	/**
	 * Registra la compra, descuenta las entradas del evento y devuelve el
	 * resumen. Todo ocurre en una sola transacción: si falla el guardado de la
	 * compra, el descuento de stock también se revierte.
	 */
	@Transactional
	public CompraResponse comprar(CompraRequest request) {
		Evento evento = eventoRepository.buscarParaActualizar(request.eventoId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe un evento con id " + request.eventoId()));

		if (request.cantidad() > evento.getEntradasDisponibles()) {
			throw new StockInsuficienteException("El evento '" + evento.getNombre() + "' solo tiene "
					+ evento.getEntradasDisponibles() + " entradas disponibles");
		}

		evento.descontarEntradas(request.cantidad());

		BigDecimal precioUnitario = evento.getPrecio();
		BigDecimal precioTotal = precioUnitario
				.multiply(BigDecimal.valueOf(request.cantidad()))
				.setScale(2, RoundingMode.HALF_UP);

		Compra compra = new Compra(evento, request.cantidad(), precioUnitario, precioTotal, Instant.now());

		return CompraResponse.desde(compraRepository.save(compra));
	}

	@Transactional(readOnly = true)
	public CompraResponse obtener(Long id) {
		return compraRepository.findById(id)
				.map(CompraResponse::desde)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe una compra con id " + id));
	}

}
