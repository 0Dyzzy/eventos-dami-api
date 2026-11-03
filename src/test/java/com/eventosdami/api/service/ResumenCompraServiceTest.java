package com.eventosdami.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import com.eventosdami.api.almacenamiento.AlmacenResumenes;
import com.eventosdami.api.almacenamiento.ResumenAlmacenado;
import com.eventosdami.api.error.ArchivoResumenInvalidoException;
import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.resumen.GeneradorResumenCompra;
import com.eventosdami.api.resumen.ResumenCompra;
import com.eventosdami.api.web.dto.CompraResponse;
import com.eventosdami.api.web.dto.EventoResponse;

@ExtendWith(MockitoExtension.class)
class ResumenCompraServiceTest {

	private static final byte[] PDF = "%PDF-1.4 resumen enviado".getBytes(StandardCharsets.US_ASCII);

	@Mock
	private CompraService compraService;

	@Mock
	private AlmacenResumenes almacen;

	private ResumenCompraService resumenService;

	@BeforeEach
	void setUp() {
		resumenService = new ResumenCompraService(compraService, new GeneradorResumenCompra(), almacen,
				DataSize.ofMegabytes(1));
	}

	@Test
	void generaElPdfDelResumenDeLaCompra() {
		when(compraService.obtener(10L)).thenReturn(compra());

		ResumenCompra resumen = resumenService.generar(10L);

		assertThat(resumen.nombreArchivo()).isEqualTo("resumen-compra-10.pdf");
		assertThat(resumen.contenido()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
		verifyNoInteractions(almacen);
	}

	@Test
	void subeElResumenGeneradoCuandoNoSeEnviaArchivo() {
		when(compraService.obtener(10L)).thenReturn(compra());
		when(almacen.subir(eq(10L), any(byte[].class))).thenReturn(almacenado());

		resumenService.subir(10L, null);

		ArgumentCaptor<byte[]> contenido = ArgumentCaptor.forClass(byte[].class);
		verify(almacen).subir(eq(10L), contenido.capture());
		assertThat(contenido.getValue()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
	}

	@Test
	void subeElArchivoRecibido() {
		when(compraService.obtener(10L)).thenReturn(compra());
		when(almacen.subir(10L, PDF)).thenReturn(almacenado());

		resumenService.subir(10L, PDF);

		verify(almacen).subir(10L, PDF);
	}

	@Test
	void rechazaUnArchivoVacio() {
		when(compraService.obtener(10L)).thenReturn(compra());

		assertThatThrownBy(() -> resumenService.subir(10L, new byte[0]))
				.isInstanceOf(ArchivoResumenInvalidoException.class)
				.hasMessageContaining("vacío");

		verify(almacen, never()).subir(anyLong(), any(byte[].class));
	}

	@Test
	void rechazaUnArchivoQueNoEsPdf() {
		when(compraService.obtener(10L)).thenReturn(compra());

		assertThatThrownBy(() -> resumenService.subir(10L, "no soy un pdf".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(ArchivoResumenInvalidoException.class)
				.hasMessageContaining("PDF");

		verify(almacen, never()).subir(anyLong(), any(byte[].class));
	}

	@Test
	void rechazaUnArchivoDemasiadoGrande() {
		when(compraService.obtener(10L)).thenReturn(compra());

		byte[] enorme = new byte[(int) DataSize.ofMegabytes(1).toBytes() + 1];
		System.arraycopy("%PDF".getBytes(StandardCharsets.US_ASCII), 0, enorme, 0, 4);

		assertThatThrownBy(() -> resumenService.subir(10L, enorme))
				.isInstanceOf(ArchivoResumenInvalidoException.class)
				.hasMessageContaining("tamaño máximo");
	}

	@Test
	void noSubeNadaSiLaCompraNoExiste() {
		when(compraService.obtener(99L))
				.thenThrow(new RecursoNoEncontradoException("No existe una compra con id 99"));

		assertThatThrownBy(() -> resumenService.subir(99L, PDF))
				.isInstanceOf(RecursoNoEncontradoException.class);

		verifyNoInteractions(almacen);
	}

	@Test
	void reemplazaElResumenConElArchivoRecibido() {
		when(compraService.obtener(10L)).thenReturn(compra());
		when(almacen.reemplazar(10L, PDF)).thenReturn(almacenado());

		assertThat(resumenService.reemplazar(10L, PDF).clave()).isEqualTo("10/resumen-compra-10.pdf");
		verify(almacen).reemplazar(10L, PDF);
	}

	@Test
	void regeneraElResumenAlReemplazarSinArchivo() {
		when(compraService.obtener(10L)).thenReturn(compra());
		when(almacen.reemplazar(eq(10L), any(byte[].class))).thenReturn(almacenado());

		resumenService.reemplazar(10L, null);

		ArgumentCaptor<byte[]> contenido = ArgumentCaptor.forClass(byte[].class);
		verify(almacen).reemplazar(eq(10L), contenido.capture());
		assertThat(contenido.getValue()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
	}

	@Test
	void descargaYEliminaElResumen() {
		when(compraService.obtener(10L)).thenReturn(compra());
		when(almacen.descargar(10L)).thenReturn(PDF);

		assertThat(resumenService.descargar(10L)).isEqualTo(PDF);
		resumenService.eliminar(10L);

		verify(almacen).eliminar(10L);
	}

	private CompraResponse compra() {
		return new CompraResponse(
				10L,
				new EventoResponse(1L, "Concierto de Rock", LocalDateTime.of(2030, 5, 20, 20, 0),
						new BigDecimal("15000.00"), 97),
				3,
				new BigDecimal("15000.00"),
				new BigDecimal("45000.00"),
				Instant.parse("2030-01-01T15:00:00Z"));
	}

	private ResumenAlmacenado almacenado() {
		return new ResumenAlmacenado("bucket", "10/resumen-compra-10.pdf", PDF.length, Instant.now(),
				"https://bucket.s3.amazonaws.com/10/resumen-compra-10.pdf");
	}

}
