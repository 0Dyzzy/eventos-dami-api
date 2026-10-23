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
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.service.EventoService;
import com.eventosdami.api.web.dto.EventoRequest;
import com.eventosdami.api.web.dto.EventoResponse;

@WebMvcTest(EventoController.class)
class EventoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventoService eventoService;

	@Test
	void listaEventosConNombreFechaYPrecio() throws Exception {
		when(eventoService.listar()).thenReturn(List.of(
				new EventoResponse(1L, "Concierto de Rock", LocalDateTime.of(2030, 5, 20, 20, 0),
						new BigDecimal("15000.00"), 100)));

		mockMvc.perform(get("/api/eventos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Concierto de Rock"))
				.andExpect(jsonPath("$[0].fecha").value("2030-05-20T20:00:00"))
				.andExpect(jsonPath("$[0].precio").value(15000.00));
	}

	@Test
	void creaEventoYDevuelve201ConLocation() throws Exception {
		when(eventoService.crear(any(EventoRequest.class))).thenReturn(
				new EventoResponse(7L, "Festival de Jazz", LocalDateTime.of(2030, 6, 1, 19, 30),
						new BigDecimal("25000.00"), 50));

		String body = """
				{
				  "nombre": "Festival de Jazz",
				  "fecha": "2030-06-01T19:30:00",
				  "precio": 25000.00,
				  "entradasDisponibles": 50
				}
				""";

		mockMvc.perform(post("/api/eventos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/eventos/7"))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.nombre").value("Festival de Jazz"))
				.andExpect(jsonPath("$.precio").value(25000.00));
	}

	@Test
	void rechazaEventoInvalidoCon400() throws Exception {
		String body = """
				{ "nombre": "", "precio": -5, "entradasDisponibles": -1 }
				""";

		mockMvc.perform(post("/api/eventos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.errores.nombre").exists())
				.andExpect(jsonPath("$.errores.fecha").exists())
				.andExpect(jsonPath("$.errores.precio").exists())
				.andExpect(jsonPath("$.errores.entradasDisponibles").exists());

		verifyNoInteractions(eventoService);
	}

	@Test
	void devuelve404CuandoElEventoNoExiste() throws Exception {
		when(eventoService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe un evento con id 99"));

		mockMvc.perform(get("/api/eventos/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe un evento con id 99"));
	}

}
