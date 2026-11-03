package com.eventosdami.api.error;

/**
 * Se lanza cuando el archivo enviado como resumen no es válido (vacío, muy
 * grande o no es un PDF).
 */
public class ArchivoResumenInvalidoException extends IllegalArgumentException {

	public ArchivoResumenInvalidoException(String mensaje) {
		super(mensaje);
	}

	public ArchivoResumenInvalidoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
