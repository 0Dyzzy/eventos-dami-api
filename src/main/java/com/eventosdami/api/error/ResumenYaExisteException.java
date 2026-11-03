package com.eventosdami.api.error;

/**
 * Se lanza cuando se intenta subir un resumen que ya estaba almacenado.
 * Para reemplazarlo hay que usar la operación de actualización.
 */
public class ResumenYaExisteException extends RuntimeException {

	public ResumenYaExisteException(String mensaje) {
		super(mensaje);
	}

}
