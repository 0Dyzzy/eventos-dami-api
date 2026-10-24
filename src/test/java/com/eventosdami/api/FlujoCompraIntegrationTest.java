package com.eventosdami.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.eventosdami.api.domain.Evento;
import com.eventosdami.api.repository.CompraRepository;
import com.eventosdami.api.repository.EventoRepository;

/**
 * Prueba el flujo completo contra la base de datos real (H2 en memoria):
 * crear evento -> listarlo -> comprar entradas -> verificar persistencia.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoCompraIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventoRepository eventoRepository;

	@Autowired
	private CompraRepository compraRepository;

	@Test
	void creaEventoLoListaYCompraEntradas() throws Exception {
		String nombre = "Concierto Integración " + UUID.randomUUID();
		String body = """
				{
				  "nombre": "%s",
				  "fecha": "2030-05-20T20:00:00",
				  "precio": 12500.50,
				  "entradasDisponibles": 100
				}
				""".formatted(nombre);

		mockMvc.perform(post("/api/eventos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value(nombre));

		Evento evento = eventoRepository.findAll().stream()
				.filter(e -> e.getNombre().equals(nombre))
				.findFirst()
				.orElseThrow(() -> new AssertionError("El evento no quedó persistido"));

		assertThat(evento.getFecha()).isEqualTo(LocalDateTime.of(2030, 5, 20, 20, 0));
		assertThat(evento.getPrecio()).isEqualByComparingTo("12500.50");

		mockMvc.perform(get("/api/eventos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '%s')].precio".formatted(nombre)).value(12500.50));

		String compraBody = """
				{ "eventoId": %d, "cantidad": 3 }
				""".formatted(evento.getId());

		mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content(compraBody))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/compras/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.evento.id").value(evento.getId()))
				.andExpect(jsonPath("$.evento.nombre").value(nombre))
				.andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.precioUnitario").value(12500.50))
				.andExpect(jsonPath("$.precioTotal").value(37501.50));

		assertThat(eventoRepository.findById(evento.getId()).orElseThrow().getEntradasDisponibles())
				.isEqualTo(97);

		assertThat(compraRepository.findByEventoIdOrderByFechaCompraDesc(evento.getId()))
				.singleElement()
				.satisfies(compra -> {
					assertThat(compra.getCantidad()).isEqualTo(3);
					assertThat(compra.getPrecioTotal()).isEqualByComparingTo("37501.50");
				});

		mockMvc.perform(get("/api/eventos/{id}", evento.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entradasDisponibles").value(97));
	}

}
