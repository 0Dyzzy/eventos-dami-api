package com.eventosdami.api.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventosdami.api.service.CompraService;
import com.eventosdami.api.web.dto.CompraRequest;
import com.eventosdami.api.web.dto.CompraResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

	private final CompraService compraService;

	public CompraController(CompraService compraService) {
		this.compraService = compraService;
	}

	/**
	 * Compra una cantidad de entradas para un evento y devuelve el resumen:
	 * evento, cantidad, precio unitario y precio total.
	 */
	@PostMapping
	public ResponseEntity<CompraResponse> comprar(@Valid @RequestBody CompraRequest request) {
		CompraResponse compra = compraService.comprar(request);
		return ResponseEntity
				.created(URI.create("/api/compras/" + compra.id()))
				.body(compra);
	}

	/** Recupera una compra almacenada. */
	@GetMapping("/{id}")
	public CompraResponse obtener(@PathVariable Long id) {
		return compraService.obtener(id);
	}

}
