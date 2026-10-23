package com.eventosdami.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.error.StockInsuficienteException;
import com.eventosdami.api.service.CompraService;
import com.eventosdami.api.web.dto.CompraRequest;
import com.eventosdami.api.web.dto.CompraResponse;
import com.eventosdami.api.web.dto.EventoResponse;

@WebMvcTest(CompraController.class)
class CompraControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompraService compraService;

	@Test
	void compraDevuelveResumenConTotal() throws Exception {
		EventoResponse evento = new EventoResponse(1L, "Concierto de Rock",
				LocalDateTime.of(2030, 5, 20, 20, 0), new BigDecimal("15000.00"), 97);

		when(compraService.comprar(any(CompraRequest.class))).thenReturn(new CompraResponse(
				10L, evento, 3, new BigDecimal("15000.00"), new BigDecimal("45000.00"),
				Instant.parse("2030-01-01T12:00:00Z")));

		String body = """
				{ "eventoId": 1, "cantidad": 3 }
				""";

		mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/compras/10"))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.evento.id").value(1))
				.andExpect(jsonPath("$.evento.nombre").value("Concierto de Rock"))
				.andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.precioUnitario").value(15000.00))
				.andExpect(jsonPath("$.precioTotal").value(45000.00))
				.andExpect(jsonPath("$.fechaCompra").value("2030-01-01T12:00:00Z"));
	}

	@Test
	void devuelve404SiElEventoNoExiste() throws Exception {
		when(compraService.comprar(any(CompraRequest.class)))
				.thenThrow(new RecursoNoEncontradoException("No existe un evento con id 99"));

		mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventoId\": 99, \"cantidad\": 2}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"));
	}

	@Test
	void devuelve409SiNoHayEntradasSuficientes() throws Exception {
		when(compraService.comprar(any(CompraRequest.class)))
				.thenThrow(new StockInsuficienteException("El evento 'Concierto' solo tiene 1 entradas disponibles"));

		mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventoId\": 1, \"cantidad\": 5}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Entradas insuficientes"))
				.andExpect(jsonPath("$.detail").value("El evento 'Concierto' solo tiene 1 entradas disponibles"));
	}

	@Test
	void validaCantidadYEvento() throws Exception {
		mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventoId\": 0, \"cantidad\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.eventoId").exists())
				.andExpect(jsonPath("$.errores.cantidad").exists());

		verifyNoInteractions(compraService);
	}

	@Test
	void obtieneCompraGuardada() throws Exception {
		EventoResponse evento = new EventoResponse(1L, "Concierto de Rock",
				LocalDateTime.of(2030, 5, 20, 20, 0), new BigDecimal("15000.00"), 97);

		when(compraService.obtener(10L)).thenReturn(new CompraResponse(
				10L, evento, 3, new BigDecimal("15000.00"), new BigDecimal("45000.00"),
				Instant.parse("2030-01-01T12:00:00Z")));

		mockMvc.perform(get("/api/compras/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.precioTotal").value(45000.00));
	}

}
