package com.eventosdami.api.error;

/**
 * Se lanza cuando se busca el resumen almacenado de una compra que no lo tiene.
 */
public class ResumenNoEncontradoException extends RuntimeException {

	public ResumenNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
