package com.eventosdami.api.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventosdami.api.domain.Evento;
import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.repository.EventoRepository;
import com.eventosdami.api.web.dto.EventoRequest;
import com.eventosdami.api.web.dto.EventoResponse;

@Service
@Transactional(readOnly = true)
public class EventoService {

	private final EventoRepository eventoRepository;

	public EventoService(EventoRepository eventoRepository) {
		this.eventoRepository = eventoRepository;
	}

	/** Lista los eventos ordenados por fecha. */
	public List<EventoResponse> listar() {
		return eventoRepository.findAll(Sort.by(Sort.Direction.ASC, "fecha"))
				.stream()
				.map(EventoResponse::desde)
				.toList();
	}

	public EventoResponse obtener(Long id) {
		return EventoResponse.desde(buscar(id));
	}

	@Transactional
	public EventoResponse crear(EventoRequest request) {
		Evento evento = new Evento(
				request.nombre(),
				request.fecha(),
				request.precio(),
				request.entradasDisponibles());

		return EventoResponse.desde(eventoRepository.save(evento));
	}

	private Evento buscar(Long id) {
		return eventoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe un evento con id " + id));
	}

}
