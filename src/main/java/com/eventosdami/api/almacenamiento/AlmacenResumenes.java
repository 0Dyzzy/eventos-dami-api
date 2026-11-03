package com.eventosdami.api.almacenamiento;

/**
 * Puerto de salida para guardar los resúmenes de compra en AWS S3
 * ({@link S3AlmacenResumenes}).
 */
public interface AlmacenResumenes {

	/**
	 * Crea el resumen de una compra.
	 *
	 * @throws com.eventosdami.api.error.ResumenYaExisteException si ya existe
	 * un resumen para la compra
	 */
	ResumenAlmacenado subir(Long compraId, byte[] contenido);

	/**
	 * Reemplaza el resumen ya existente de una compra.
	 *
	 * @throws com.eventosdami.api.error.ResumenNoEncontradoException si la
	 * compra no tiene un resumen almacenado
	 */
	ResumenAlmacenado reemplazar(Long compraId, byte[] contenido);

	/**
	 * Metadatos del resumen almacenado.
	 *
	 * @throws com.eventosdami.api.error.ResumenNoEncontradoException si no existe
	 */
	ResumenAlmacenado obtener(Long compraId);

	/**
	 * Contenido del resumen almacenado.
	 *
	 * @throws com.eventosdami.api.error.ResumenNoEncontradoException si no existe
	 */
	byte[] descargar(Long compraId);

	/**
	 * Elimina el resumen almacenado.
	 *
	 * @throws com.eventosdami.api.error.ResumenNoEncontradoException si no existe
	 */
	void eliminar(Long compraId);

	/** Indica si la compra ya tiene un resumen almacenado. */
	boolean existe(Long compraId);

}
