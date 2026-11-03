package com.eventosdami.api.web.dto;

import java.time.Instant;

import com.eventosdami.api.almacenamiento.ResumenAlmacenado;

/**
 * Respuesta con la ubicación y metadatos del resumen guardado.
 */
public record ResumenAlmacenadoResponse(
		Long compraId,
		String bucket,
		String carpeta,
		String clave,
		String nombreArchivo,
		long tamanio,
		Instant ultimaModificacion,
		String url) {

	public static ResumenAlmacenadoResponse desde(Long compraId, ResumenAlmacenado resumen) {
		return new ResumenAlmacenadoResponse(
				compraId,
				resumen.bucket(),
				resumen.carpeta(),
				resumen.clave(),
				resumen.nombreArchivo(),
				resumen.tamanio(),
				resumen.ultimaModificacion(),
				resumen.url());
	}

}
