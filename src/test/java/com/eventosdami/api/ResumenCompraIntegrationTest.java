package com.eventosdami.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eventosdami.api.domain.Evento;
import com.eventosdami.api.repository.EventoRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Flujo completo del resumen de compra: generar el PDF, subirlo, descargarlo,
 * reemplazarlo y eliminarlo. El bucket se sustituye por un doble en memoria;
 * no llama a AWS.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResumenCompraIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventoRepository eventoRepository;

	@Test
	void generaSubeDescargaReemplazaYEliminaElResumen() throws Exception {
		long compraId = registrarCompra();

		// 1. Genera el archivo PDF para guardarlo en el computador.
		MvcResult generado = mockMvc.perform(get("/api/compras/{id}/resumen", compraId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("resumen-compra-" + compraId + ".pdf")))
				.andReturn();

		byte[] pdf = generado.getResponse().getContentAsByteArray();
		assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

		// 2. Sube el resumen generado (sin enviar archivo) al almacenamiento.
		mockMvc.perform(post("/api/compras/{id}/resumen", compraId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.compraId").value((int) compraId))
				.andExpect(jsonPath("$.carpeta").value(String.valueOf(compraId)))
				.andExpect(jsonPath("$.clave").value(compraId + "/resumen-compra-" + compraId + ".pdf"));

		mockMvc.perform(get("/api/compras/{id}/resumen/info", compraId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.carpeta").value(String.valueOf(compraId)))
				.andExpect(jsonPath("$.clave").value(compraId + "/resumen-compra-" + compraId + ".pdf"));

		// Subir dos veces el mismo resumen no está permitido.
		mockMvc.perform(post("/api/compras/{id}/resumen", compraId))
				.andExpect(status().isConflict());

		// 3. Descarga el resumen almacenado.
		byte[] descargado = mockMvc.perform(get("/api/compras/{id}/resumen/s3", compraId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		assertThat(descargado).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

		// 4. Reemplaza el resumen por una versión corregida.
		byte[] corregido = "%PDF-1.7 resumen corregido de la compra 10".getBytes(StandardCharsets.US_ASCII);
		MockMultipartFile archivo = new MockMultipartFile("archivo", "resumen-corregido.pdf",
				MediaType.APPLICATION_PDF_VALUE, corregido);

		mockMvc.perform(multipart("/api/compras/{id}/resumen", compraId)
				.file(archivo)
				.with(peticion -> {
					peticion.setMethod("PUT");
					return peticion;
				}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tamanio").value(corregido.length));

		assertThat(mockMvc.perform(get("/api/compras/{id}/resumen/s3", compraId))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsByteArray()).isEqualTo(corregido);

		// 5. Elimina el resumen.
		mockMvc.perform(delete("/api/compras/{id}/resumen", compraId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/compras/{id}/resumen/s3", compraId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Resumen no encontrado"));

		// El resumen se puede volver a generar desde la compra registrada.
		mockMvc.perform(get("/api/compras/{id}/resumen", compraId))
				.andExpect(status().isOk());
	}

	private long registrarCompra() throws Exception {
		Evento evento = eventoRepository.save(new Evento("Concierto Integración " + UUID.randomUUID(),
				LocalDateTime.of(2030, 5, 20, 20, 0), new BigDecimal("15000.00"), 100));

		MvcResult compra = mockMvc.perform(post("/api/compras")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventoId\": %d, \"cantidad\": 3}".formatted(evento.getId())))
				.andExpect(status().isCreated())
				.andReturn();

		Number id = JsonPath.read(compra.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
