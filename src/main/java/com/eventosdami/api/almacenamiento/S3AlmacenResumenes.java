package com.eventosdami.api.almacenamiento;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;

import com.eventosdami.api.error.ResumenNoEncontradoException;
import com.eventosdami.api.error.ResumenYaExisteException;
import com.eventosdami.api.resumen.ClaveResumen;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Guarda los resúmenes de compra en un bucket de AWS S3 usando {@link S3Operations}
 * (Spring Cloud AWS).
 * <p>
 * Cada resumen queda en una carpeta cuyo nombre es el número del resumen:
 * {@code <bucket>/<id-compra>/resumen-compra-<id-compra>.pdf}.
 */
public class S3AlmacenResumenes implements AlmacenResumenes {

	private final S3Operations s3;

	private final String bucket;

	public S3AlmacenResumenes(S3Operations s3, String bucket) {
		this.s3 = s3;
		this.bucket = bucket;
	}

	@Override
	public ResumenAlmacenado subir(Long compraId, byte[] contenido) {
		if (existe(compraId)) {
			throw new ResumenYaExisteException("La compra " + compraId
					+ " ya tiene un resumen guardado en S3; use PUT para reemplazarlo");
		}
		return escribir(compraId, contenido);
	}

	@Override
	public ResumenAlmacenado reemplazar(Long compraId, byte[] contenido) {
		if (!existe(compraId)) {
			throw noEncontrado(compraId);
		}
		return escribir(compraId, contenido);
	}

	@Override
	public ResumenAlmacenado obtener(Long compraId) {
		S3Resource recurso = recursoExistente(compraId);

		return new ResumenAlmacenado(bucket, ClaveResumen.clave(compraId), recurso.contentLength(),
				Instant.ofEpochMilli(recurso.lastModified()), url(recurso));
	}

	@Override
	public byte[] descargar(Long compraId) {
		S3Resource recurso = recursoExistente(compraId);

		try (InputStream entrada = recurso.getInputStream()) {
			return entrada.readAllBytes();
		}
		catch (NoSuchKeyException ex) {
			throw noEncontrado(compraId);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo leer el resumen " + compraId + " desde S3", ex);
		}
	}

	@Override
	public void eliminar(Long compraId) {
		if (!existe(compraId)) {
			throw noEncontrado(compraId);
		}

		s3.deleteObject(bucket, ClaveResumen.clave(compraId));
	}

	@Override
	public boolean existe(Long compraId) {
		return s3.objectExists(bucket, ClaveResumen.clave(compraId));
	}

	private ResumenAlmacenado escribir(Long compraId, byte[] contenido) {
		String clave = ClaveResumen.clave(compraId);

		ObjectMetadata metadatos = ObjectMetadata.builder()
				.contentType(ClaveResumen.TIPO_CONTENIDO)
				.contentLength((long) contenido.length)
				.build();

		S3Resource recurso = s3.upload(bucket, clave, new ByteArrayInputStream(contenido), metadatos);

		return new ResumenAlmacenado(bucket, clave, contenido.length, Instant.now(), url(recurso));
	}

	private S3Resource recursoExistente(Long compraId) {
		S3Resource recurso = s3.download(bucket, ClaveResumen.clave(compraId));

		if (!recurso.exists()) {
			throw noEncontrado(compraId);
		}
		return recurso;
	}

	private String url(S3Resource recurso) {
		try {
			return recurso.getURL().toString();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo obtener la URL del resumen en S3", ex);
		}
	}

	private ResumenNoEncontradoException noEncontrado(Long compraId) {
		return new ResumenNoEncontradoException(
				"No hay un resumen guardado en S3 para la compra " + compraId);
	}

}
