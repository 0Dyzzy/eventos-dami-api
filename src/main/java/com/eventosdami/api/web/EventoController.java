package com.eventosdami.api.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventosdami.api.service.EventoService;
import com.eventosdami.api.web.dto.EventoRequest;
import com.eventosdami.api.web.dto.EventoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService eventoService;

	public EventoController(EventoService eventoService) {
		this.eventoService = eventoService;
	}

	/** Lista los eventos disponibles con nombre, fecha y precio. */
	@GetMapping
	public List<EventoResponse> listar() {
		return eventoService.listar();
	}

	@GetMapping("/{id}")
	public EventoResponse obtener(@PathVariable Long id) {
		return eventoService.obtener(id);
	}

	/** Crea un nuevo evento y lo persiste en la base de datos. */
	@PostMapping
	public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest request) {
		EventoResponse creado = eventoService.crear(request);
		return ResponseEntity
				.created(URI.create("/api/eventos/" + creado.id()))
				.body(creado);
	}

}
