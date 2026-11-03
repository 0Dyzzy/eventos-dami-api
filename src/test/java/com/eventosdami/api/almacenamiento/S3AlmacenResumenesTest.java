package com.eventosdami.api.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventosdami.api.error.ResumenNoEncontradoException;
import com.eventosdami.api.error.ResumenYaExisteException;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;

@ExtendWith(MockitoExtension.class)
class S3AlmacenResumenesTest {

	private static final String BUCKET = "eventos-dami-resumenes";

	private static final String CLAVE = "10/resumen-compra-10.pdf";

	private static final byte[] PDF = "%PDF-1.4 resumen".getBytes(StandardCharsets.US_ASCII);

	@Mock
	private S3Operations s3;

	@Mock
	private S3Resource recurso;

	private S3AlmacenResumenes almacen;

	private URL urlObjeto;

	@BeforeEach
	void setUp() throws Exception {
		almacen = new S3AlmacenResumenes(s3, BUCKET);
		urlObjeto = URI.create("https://" + BUCKET + ".s3.amazonaws.com/" + CLAVE).toURL();
		lenient().when(recurso.getURL()).thenReturn(urlObjeto);
	}

	@Test
	void subeElResumenEnLaCarpetaConElNumeroDelResumen() throws Exception {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(false);
		cuandoSube();

		ResumenAlmacenado almacenado = almacen.subir(10L, PDF);

		ArgumentCaptor<InputStream> entrada = ArgumentCaptor.forClass(InputStream.class);
		ArgumentCaptor<ObjectMetadata> metadatos = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(s3).upload(eq(BUCKET), eq(CLAVE), entrada.capture(), metadatos.capture());

		assertThat(entrada.getValue().readAllBytes()).isEqualTo(PDF);
		assertThat(metadatos.getValue().getContentType()).isEqualTo("application/pdf");
		assertThat(metadatos.getValue().getContentLength()).isEqualTo(PDF.length);

		assertThat(almacenado.bucket()).isEqualTo(BUCKET);
		assertThat(almacenado.clave()).isEqualTo(CLAVE);
		assertThat(almacenado.carpeta()).isEqualTo("10");
		assertThat(almacenado.nombreArchivo()).isEqualTo("resumen-compra-10.pdf");
		assertThat(almacenado.tamanio()).isEqualTo(PDF.length);
		assertThat(almacenado.url()).isEqualTo(urlObjeto.toString());
	}

	@Test
	void noSubeUnResumenQueYaExiste() {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(true);

		assertThatThrownBy(() -> almacen.subir(10L, PDF))
				.isInstanceOf(ResumenYaExisteException.class)
				.hasMessageContaining("compra 10");

		verify(s3, never()).upload(any(), any(), any(), any());
	}

	@Test
	void reemplazaElResumenExistente() {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(true);
		cuandoSube();

		assertThat(almacen.reemplazar(10L, PDF).clave()).isEqualTo(CLAVE);

		verify(s3).upload(eq(BUCKET), eq(CLAVE), any(InputStream.class), any(ObjectMetadata.class));
	}

	@Test
	void noReemplazaUnResumenInexistente() {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(false);

		assertThatThrownBy(() -> almacen.reemplazar(10L, PDF))
				.isInstanceOf(ResumenNoEncontradoException.class);

		verify(s3, never()).upload(any(), any(), any(), any());
	}

	@Test
	void descargaElContenidoDelResumen() throws Exception {
		cuandoExiste();
		lenient().when(recurso.getInputStream()).thenReturn(new ByteArrayInputStream(PDF));

		assertThat(almacen.descargar(10L)).isEqualTo(PDF);
	}

	@Test
	void informaCuandoElResumenNoExisteEnS3() {
		cuandoNoExiste();

		assertThatThrownBy(() -> almacen.descargar(10L))
				.isInstanceOf(ResumenNoEncontradoException.class)
				.hasMessageContaining("compra 10");

		assertThatThrownBy(() -> almacen.obtener(10L))
				.isInstanceOf(ResumenNoEncontradoException.class);
	}

	@Test
	void obtieneLosMetadatosDelResumen() {
		cuandoExiste();
		Instant modificado = Instant.parse("2030-01-01T12:00:00Z");
		when(recurso.contentLength()).thenReturn((long) PDF.length);
		when(recurso.lastModified()).thenReturn(modificado.toEpochMilli());

		ResumenAlmacenado almacenado = almacen.obtener(10L);

		assertThat(almacenado.clave()).isEqualTo(CLAVE);
		assertThat(almacenado.tamanio()).isEqualTo(PDF.length);
		assertThat(almacenado.ultimaModificacion()).isEqualTo(modificado);
		assertThat(almacenado.url()).isEqualTo(urlObjeto.toString());
	}

	@Test
	void eliminaElResumenExistente() {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(true);

		almacen.eliminar(10L);

		verify(s3).deleteObject(BUCKET, CLAVE);
	}

	@Test
	void noEliminaUnResumenInexistente() {
		when(s3.objectExists(BUCKET, CLAVE)).thenReturn(false);

		assertThatThrownBy(() -> almacen.eliminar(10L))
				.isInstanceOf(ResumenNoEncontradoException.class);

		verify(s3, never()).deleteObject(any(), any());
	}

	private void cuandoSube() {
		lenient().when(s3.upload(eq(BUCKET), eq(CLAVE), any(InputStream.class), any(ObjectMetadata.class)))
				.thenReturn(recurso);
	}

	private void cuandoExiste() {
		lenient().when(s3.objectExists(BUCKET, CLAVE)).thenReturn(true);
		lenient().when(s3.download(BUCKET, CLAVE)).thenReturn(recurso);
		lenient().when(recurso.exists()).thenReturn(true);
	}

	private void cuandoNoExiste() {
		lenient().when(s3.objectExists(BUCKET, CLAVE)).thenReturn(false);
		lenient().when(s3.download(BUCKET, CLAVE)).thenReturn(recurso);
		lenient().when(recurso.exists()).thenReturn(false);
	}

}
