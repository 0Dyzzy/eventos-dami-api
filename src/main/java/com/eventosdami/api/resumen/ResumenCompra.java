package com.eventosdami.api.resumen;

/**
 * Archivo de resumen de compra generado en memoria, listo para descargarse
 * o subirse a un almacenamiento externo.
 *
 * @param nombreArchivo nombre sugerido para el archivo (por ejemplo
 * {@code resumen-compra-10.pdf})
 * @param tipoContenido tipo MIME del archivo ({@code application/pdf})
 * @param contenido bytes del archivo
 */
public record ResumenCompra(String nombreArchivo, String tipoContenido, byte[] contenido) {

	public long tamanio() {
		return contenido.length;
	}

}
