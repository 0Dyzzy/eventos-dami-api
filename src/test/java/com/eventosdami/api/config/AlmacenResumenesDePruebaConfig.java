package com.eventosdami.api.config;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.eventosdami.api.almacenamiento.AlmacenResumenes;
import com.eventosdami.api.almacenamiento.ResumenAlmacenado;
import com.eventosdami.api.error.ResumenNoEncontradoException;
import com.eventosdami.api.error.ResumenYaExisteException;
import com.eventosdami.api.resumen.ClaveResumen;

/**
 * Sustituye el bucket de S3 en los tests. No forma parte de la aplicación.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "false")
public class AlmacenResumenesDePruebaConfig {

	@Bean
	AlmacenResumenes almacenResumenesDePrueba() {
		return new AlmacenEnMemoria();
	}

	private static final class AlmacenEnMemoria implements AlmacenResumenes {

		private final Map<Long, byte[]> objetos = new ConcurrentHashMap<>();

		@Override
		public ResumenAlmacenado subir(Long compraId, byte[] contenido) {
			if (objetos.containsKey(compraId)) {
				throw new ResumenYaExisteException("La compra " + compraId
						+ " ya tiene un resumen guardado en S3; use PUT para reemplazarlo");
			}
			return guardar(compraId, contenido);
		}

		@Override
		public ResumenAlmacenado reemplazar(Long compraId, byte[] contenido) {
			exigir(compraId);
			return guardar(compraId, contenido);
		}

		@Override
		public ResumenAlmacenado obtener(Long compraId) {
			byte[] contenido = exigir(compraId);
			return metadatos(compraId, contenido.length);
		}

		@Override
		public byte[] descargar(Long compraId) {
			return Arrays.copyOf(exigir(compraId), exigir(compraId).length);
		}

		@Override
		public void eliminar(Long compraId) {
			if (objetos.remove(compraId) == null) {
				throw noEncontrado(compraId);
			}
		}

		@Override
		public boolean existe(Long compraId) {
			return objetos.containsKey(compraId);
		}

		private ResumenAlmacenado guardar(Long compraId, byte[] contenido) {
			objetos.put(compraId, Arrays.copyOf(contenido, contenido.length));
			return metadatos(compraId, contenido.length);
		}

		private byte[] exigir(Long compraId) {
			byte[] contenido = objetos.get(compraId);
			if (contenido == null) {
				throw noEncontrado(compraId);
			}
			return contenido;
		}

		private ResumenAlmacenado metadatos(Long compraId, long tamanio) {
			String clave = ClaveResumen.clave(compraId);
			return new ResumenAlmacenado("eventos-dami-test", clave, tamanio, Instant.now(),
					"https://eventos-dami-test.s3.us-east-1.amazonaws.com/" + clave);
		}

		private ResumenNoEncontradoException noEncontrado(Long compraId) {
			return new ResumenNoEncontradoException(
					"No hay un resumen guardado en S3 para la compra " + compraId);
		}

	}

}
