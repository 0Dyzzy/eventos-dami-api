package com.eventosdami.api.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventosdami.api.almacenamiento.ResumenAlmacenado;
import com.eventosdami.api.error.ArchivoResumenInvalidoException;
import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.error.ResumenNoEncontradoException;
import com.eventosdami.api.error.ResumenYaExisteException;
import com.eventosdami.api.resumen.ResumenCompra;
import com.eventosdami.api.service.ResumenCompraService;

@WebMvcTest(ResumenCompraController.class)
class ResumenCompraControllerTest {

	private static final byte[] PDF = "%PDF-1.4 resumen de prueba".getBytes(StandardCharsets.US_ASCII);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumenCompraService resumenService;

	@Test
	void generaYDescargaElResumenComoArchivoPdf() throws Exception {
		when(resumenService.generar(10L))
				.thenReturn(new ResumenCompra("resumen-compra-10.pdf", "application/pdf", PDF));

		mockMvc.perform(get("/api/compras/10/resumen"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("attachment")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("resumen-compra-10.pdf")))
				.andExpect(content().bytes(PDF));
	}

	@Test
	void subeElResumenSinArchivoYDevuelve201() throws Exception {
		when(resumenService.subir(10L, null)).thenReturn(almacenado());

		mockMvc.perform(post("/api/compras/10/resumen"))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, "/api/compras/10/resumen/s3"))
				.andExpect(jsonPath("$.compraId").value(10))
				.andExpect(jsonPath("$.bucket").value("bucket"))
				.andExpect(jsonPath("$.carpeta").value("10"))
				.andExpect(jsonPath("$.clave").value("10/resumen-compra-10.pdf"))
				.andExpect(jsonPath("$.tamanio").value(PDF.length));

		verify(resumenService).subir(10L, null);
	}

	@Test
	void subeElArchivoEnviadoEnElMultipart() throws Exception {
		when(resumenService.subir(10L, PDF)).thenReturn(almacenado());

		MockMultipartFile archivo = new MockMultipartFile("archivo", "resumen-compra-10.pdf",
				MediaType.APPLICATION_PDF_VALUE, PDF);

		mockMvc.perform(multipart("/api/compras/10/resumen").file(archivo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.carpeta").value("10"));

		verify(resumenService).subir(10L, PDF);
	}

	@Test
	void reemplazaElResumenGuardado() throws Exception {
		when(resumenService.reemplazar(10L, PDF)).thenReturn(almacenado());

		MockMultipartFile archivo = new MockMultipartFile("archivo", "resumen-compra-10.pdf",
				MediaType.APPLICATION_PDF_VALUE, PDF);

		mockMvc.perform(multipart("/api/compras/10/resumen")
				.file(archivo)
				.with(peticion -> {
					peticion.setMethod("PUT");
					return peticion;
				}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clave").value("10/resumen-compra-10.pdf"));

		verify(resumenService).reemplazar(10L, PDF);
	}

	@Test
	void descargaElResumenGuardadoEnS3() throws Exception {
		when(resumenService.descargar(10L)).thenReturn(PDF);

		mockMvc.perform(get("/api/compras/10/resumen/s3"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						containsString("resumen-compra-10.pdf")))
				.andExpect(content().bytes(PDF));
	}

	@Test
	void devuelveLosMetadatosDelResumenGuardado() throws Exception {
		when(resumenService.obtener(10L)).thenReturn(almacenado());

		mockMvc.perform(get("/api/compras/10/resumen/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.carpeta").value("10"))
				.andExpect(jsonPath("$.clave").value("10/resumen-compra-10.pdf"))
				.andExpect(jsonPath("$.url").value(almacenado().url()));
	}

	@Test
	void eliminaElResumen() throws Exception {
		mockMvc.perform(delete("/api/compras/10/resumen"))
				.andExpect(status().isNoContent());

		verify(resumenService).eliminar(10L);
	}

	@Test
	void devuelve404SiLaCompraNoExiste() throws Exception {
		when(resumenService.generar(99L))
				.thenThrow(new RecursoNoEncontradoException("No existe una compra con id 99"));

		mockMvc.perform(get("/api/compras/99/resumen"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"));
	}

	@Test
	void devuelve409SiElResumenYaExiste() throws Exception {
		when(resumenService.subir(eq(10L), any()))
				.thenThrow(new ResumenYaExisteException("La compra 10 ya tiene un resumen guardado"));

		mockMvc.perform(post("/api/compras/10/resumen"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("El resumen ya existe"));
	}

	@Test
	void devuelve400SiElArchivoNoEsValido() throws Exception {
		when(resumenService.subir(eq(10L), any()))
				.thenThrow(new ArchivoResumenInvalidoException("El archivo del resumen debe ser un PDF"));

		MockMultipartFile archivo = new MockMultipartFile("archivo", "notas.txt",
				MediaType.TEXT_PLAIN_VALUE, "no soy un pdf".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/compras/10/resumen").file(archivo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Archivo inválido"));
	}

	@Test
	void devuelve404SiElResumenNoEstaEnElAlmacenamiento() throws Exception {
		when(resumenService.descargar(10L))
				.thenThrow(new ResumenNoEncontradoException("No hay un resumen guardado para la compra 10"));

		mockMvc.perform(get("/api/compras/10/resumen/s3"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Resumen no encontrado"));
	}

	private ResumenAlmacenado almacenado() {
		return new ResumenAlmacenado("bucket", "10/resumen-compra-10.pdf", PDF.length,
				Instant.parse("2030-01-01T12:00:00Z"),
				"https://bucket.s3.amazonaws.com/10/resumen-compra-10.pdf");
	}

}
