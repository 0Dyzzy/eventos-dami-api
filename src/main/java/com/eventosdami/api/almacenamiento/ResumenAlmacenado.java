package com.eventosdami.api.almacenamiento;

import java.time.Instant;

/**
 * Metadatos del archivo de resumen que quedó guardado en el bucket de S3.
 *
 * @param bucket nombre del bucket
 * @param clave ruta del objeto dentro del bucket, por ejemplo
 * {@code 10/resumen-compra-10.pdf}
 * @param tamanio tamaño en bytes
 * @param ultimaModificacion fecha de la última escritura
 * @param url ubicación del objeto (útil para revisarlo en la consola de AWS)
 */
public record ResumenAlmacenado(
		String bucket,
		String clave,
		long tamanio,
		Instant ultimaModificacion,
		String url) {

	/** Carpeta que contiene el resumen; corresponde al número del resumen. */
	public String carpeta() {
		int separador = clave.indexOf('/');
		return separador < 0 ? "" : clave.substring(0, separador);
	}

	/** Nombre del archivo dentro de la carpeta. */
	public String nombreArchivo() {
		int separador = clave.lastIndexOf('/');
		return separador < 0 ? clave : clave.substring(separador + 1);
	}

}
